/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
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

async function parseAuthError(response: Response, fallback: string): Promise<Error> {
	let message = fallback;
	try {
		const body = (await response.json()) as AuthErrorBody;
		if (body.message) {
			message = body.message;
		}
	} catch {
		/* keep default message */
	}
	return new Error(message);
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
