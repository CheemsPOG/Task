/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

import { clearToken, getAcceptLanguage, getToken, refreshAccessToken } from './auth.ts';

const API_BASE = '/api';

export type ApiErrorBody = {
	errorCode?: string;
	message?: string;
};

export class ApiHttpError extends Error {
	readonly status: number;
	readonly errorCode?: string;

	constructor(status: number, message: string, errorCode?: string) {
		super(message);
		this.name = 'ApiHttpError';
		this.status = status;
		this.errorCode = errorCode;
	}
}

type UnauthorizedHandler = () => void;

let onUnauthorized: UnauthorizedHandler | null = null;
let refreshInFlight: Promise<boolean> | null = null;

/**
 * Registers a handler for HTTP 401 from authenticated API calls.
 *
 * @param handler callback (typically logout + show login)
 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
	onUnauthorized = handler;
}

function buildHeaders(extra: Record<string, string> = {}): HeadersInit {
	const headers: Record<string, string> = {
		'Accept-Language': getAcceptLanguage(),
		...extra,
	};
	const token = getToken();
	if (token) {
		headers.Authorization = `Bearer ${token}`;
	}
	return headers;
}

async function parseError(response: Response): Promise<ApiHttpError> {
	let message = `HTTP ${response.status}`;
	let errorCode: string | undefined;
	try {
		const body = (await response.json()) as ApiErrorBody;
		if (body.message) {
			message = body.message;
		}
		errorCode = body.errorCode;
	} catch {
		/* keep default */
	}
	return new ApiHttpError(response.status, message, errorCode);
}

function handleUnauthorizedStatus(status: number): void {
	if (status !== 401) {
		return;
	}
	clearToken();
	onUnauthorized?.();
}

async function tryRefreshOnce(): Promise<boolean> {
	if (!refreshInFlight) {
		refreshInFlight = refreshAccessToken()
			.then(() => true)
			.catch(() => false)
			.finally(() => {
				refreshInFlight = null;
			});
	}
	return refreshInFlight;
}

type FetchOptions = RequestInit & {
	retryOnUnauthorized?: boolean;
};

async function fetchWithAuth(url: URL, options: FetchOptions = {}): Promise<Response> {
	const { retryOnUnauthorized = true, ...fetchOptions } = options;
	const response = await fetch(url, {
		...fetchOptions,
		credentials: 'include',
		headers: buildHeaders(
			(fetchOptions.headers as Record<string, string> | undefined) ?? {}
		),
	});

	if (response.status === 401 && retryOnUnauthorized) {
		const refreshed = await tryRefreshOnce();
		if (refreshed) {
			const retryResponse = await fetch(url, {
				...fetchOptions,
				credentials: 'include',
				headers: buildHeaders(
					(fetchOptions.headers as Record<string, string> | undefined) ?? {}
				),
			});
			if (retryResponse.status === 401) {
				handleUnauthorizedStatus(retryResponse.status);
			}
			return retryResponse;
		}
		handleUnauthorizedStatus(response.status);
	}

	return response;
}

/**
 * GET JSON from an authenticated backend path (e.g. `/curpairs`).
 *
 * @param path absolute path on the same origin
 * @returns parsed JSON
 */
export async function fetchAuthenticatedJson<T>(path: string): Promise<T> {
	const url = new URL(path, window.location.origin);

	try {
		const response = await fetchWithAuth(url);
		if (!response.ok) {
			throw await parseError(response);
		}

		return response.json() as Promise<T>;
	} catch (error) {
		if (error instanceof Error) {
			throw error;
		}
		throw new Error(`Request failed for ${url.pathname}`);
	}
}

/**
 * GET JSON from the chart backend.
 *
 * @param path path under `/api`
 * @param params query params
 * @returns parsed JSON
 */
export async function apiGet<T>(
	path: string,
	params: Record<string, string | number | boolean | null | undefined> = {}
): Promise<T> {
	const url = new URL(`${API_BASE}${path}`, window.location.origin);

	Object.entries(params).forEach(([key, value]) => {
		if (value !== undefined && value !== null && value !== '') {
			url.searchParams.set(key, String(value));
		}
	});

	try {
		const response = await fetchWithAuth(url);
		if (!response.ok) {
			throw await parseError(response);
		}

		return response.json() as Promise<T>;
	} catch (error) {
		if (error instanceof Error) {
			throw error;
		}
		throw new Error(`Request failed for ${url.pathname}`);
	}
}

/**
 * POST JSON to the chart backend (authenticated).
 *
 * @param path path under `/api`
 * @param body request body
 * @returns parsed JSON
 */
export async function apiPost<T>(path: string, body: unknown): Promise<T> {
	const url = new URL(`${API_BASE}${path}`, window.location.origin);

	try {
		const response = await fetchWithAuth(url, {
			method: 'POST',
			headers: buildHeaders({ 'Content-Type': 'application/json' }),
			body: JSON.stringify(body),
		});
		if (!response.ok) {
			throw await parseError(response);
		}

		return response.json() as Promise<T>;
	} catch (error) {
		if (error instanceof Error) {
			throw error;
		}
		throw new Error(`Request failed for ${url.pathname}`);
	}
}
