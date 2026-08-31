/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Local JWT stand-in for Peach S-01 (not the real Peach token service).
 *
 * Access token: JWT string in sessionStorage (`chart_access_token`). Tab-scoped
 * so closing the tab logs the user out of the access token.
 *
 * Refresh token: HttpOnly cookie `chart_refresh_token` set by Java on login.
 * This file never reads that cookie in JS — it only POSTs /api/auth/refresh
 * with credentials: 'include' and stores the new accessToken.
 *
 * Default demo accounts (seeded by AppUserSeedRunner): demo/demo, demo2/demo2.
 */

const TOKEN_KEY = 'chart_access_token';

export type LoginResponse = {
	accessToken: string;
	tokenType: string;
	expiresIn: number;
	refreshExpiresIn: number;
};

export type RefreshResponse = {
	accessToken: string;
	tokenType: string;
	expiresIn: number;
};

export type AuthErrorBody = {
	errorCode?: string;
	message?: string;
};

/**
 * @returns stored JWT, or null if logged out
 */
export function getToken(): string | null {
	try {
		const token = sessionStorage.getItem(TOKEN_KEY);
		return token && token.length > 0 ? token : null;
	} catch {
		return null;
	}
}

/**
 * Persists the access token for this browser tab.
 *
 * @param token JWT access token
 */
export function setToken(token: string): void {
	sessionStorage.setItem(TOKEN_KEY, token);
}

/**
 * Clears the stored access token.
 */
export function clearToken(): void {
	sessionStorage.removeItem(TOKEN_KEY);
}

/**
 * Preferred Accept-Language for API error messages.
 *
 * @returns `ja` or `en`
 */
export function getAcceptLanguage(): string {
	const language = navigator.language || 'en';
	return language.toLowerCase().startsWith('ja') ? 'ja' : 'en';
}

function authHeaders(extra: Record<string, string> = {}): HeadersInit {
	return {
		'Accept-Language': getAcceptLanguage(),
		...extra,
	};
}

/**
 * Reads `{ message, errorCode }` from a failed JSON response; keeps `fallback`
 * when the body is empty or not JSON. Login uses a status-specific fallback;
 * api.ts uses `HTTP ${status}`.
 *
 * @param response failed HTTP response
 * @param fallback message when the body has no `message`
 * @returns parsed message and optional error code
 */
export async function readErrorBody(
	response: Response,
	fallback: string
): Promise<{ message: string; errorCode?: string }> {
	let message = fallback;
	let errorCode: string | undefined;
	try {
		const body = (await response.json()) as AuthErrorBody;
		if (body.message) {
			message = body.message;
		}
		errorCode = body.errorCode;
	} catch {
		// Keep fallback when the body is empty or not JSON.
	}
	return { message, errorCode };
}

async function parseAuthError(response: Response, fallback: string): Promise<Error> {
	const parsed = await readErrorBody(response, fallback);
	return new Error(parsed.message);
}

/**
 * Logs in and stores the Bearer token.
 *
 * @param username login name
 * @param password password
 * @throws Error with localized message on failure
 */
export async function login(username: string, password: string): Promise<LoginResponse> {
	const response = await fetch('/api/auth/login', {
		method: 'POST',
		credentials: 'include',
		headers: authHeaders({ 'Content-Type': 'application/json' }),
		body: JSON.stringify({ username, password }),
	});

	if (!response.ok) {
		throw await parseAuthError(response, `Login failed (${response.status})`);
	}

	const payload = (await response.json()) as LoginResponse;
	if (!payload.accessToken) {
		throw new Error('Login response missing accessToken');
	}

	setToken(payload.accessToken);
	return payload;
}

/**
 * Exchanges the HttpOnly refresh cookie for a new access token.
 *
 * @throws Error when refresh fails
 */
export async function refreshAccessToken(): Promise<RefreshResponse> {
	const response = await fetch('/api/auth/refresh', {
		method: 'POST',
		credentials: 'include',
		headers: authHeaders(),
	});

	if (!response.ok) {
		throw await parseAuthError(response, `Refresh failed (${response.status})`);
	}

	const payload = (await response.json()) as RefreshResponse;
	if (!payload.accessToken) {
		throw new Error('Refresh response missing accessToken');
	}

	setToken(payload.accessToken);
	return payload;
}

/**
 * Revokes the refresh cookie server-side, then clears the access token.
 */
export async function logout(): Promise<void> {
	try {
		await fetch('/api/auth/logout', {
			method: 'POST',
			credentials: 'include',
			headers: authHeaders(),
		});
	} catch {
		/* best effort */
	}
	clearToken();
}
