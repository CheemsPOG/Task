/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

const API_BASE = '/api';
const CUSTOMER_NO_HEADER = 'X-Customer-No';
const DEFAULT_CUSTOMER_NO = '1';

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
		const response = await fetch(url, {
			headers: {
				[CUSTOMER_NO_HEADER]: DEFAULT_CUSTOMER_NO,
			},
		});
		if (!response.ok) {
			throw new Error(`HTTP ${response.status} for ${url.pathname}`);
		}

		return response.json() as Promise<T>;
	} catch (error) {
		if (error instanceof Error) {
			throw error;
		}
		throw new Error(`Request failed for ${url.pathname}`);
	}
}
