const API_BASE = '/api';

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

	const response = await fetch(url);
	if (!response.ok) {
		throw new Error(`HTTP ${response.status} for ${url.pathname}`);
	}

	return response.json() as Promise<T>;
}
