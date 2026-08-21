/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

const TOKEN_KEY = 'chart_access_token';

export type LoginResponse = {
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
		headers: {
			'Content-Type': 'application/json',
			'Accept-Language': getAcceptLanguage(),
		},
		body: JSON.stringify({ username, password }),
	});

	if (!response.ok) {
		let message = `Login failed (${response.status})`;
		try {
			const body = (await response.json()) as AuthErrorBody;
			if (body.message) {
				message = body.message;
			}
		} catch {
			/* keep default message */
		}
		throw new Error(message);
	}

	const payload = (await response.json()) as LoginResponse;
	if (!payload.accessToken) {
		throw new Error('Login response missing accessToken');
	}

	setToken(payload.accessToken);
	return payload;
}

/**
 * Clears the session token (caller should re-show login / reload).
 */
export function logout(): void {
	clearToken();
}
