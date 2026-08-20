import { parseCurrencyPairs } from './quoteStore.ts';
import type { CurrencyPair } from './types.ts';

export async function fetchCurpairs(): Promise<CurrencyPair[]> {
	const response = await fetch('/curpairs');
	if (!response.ok) {
		throw new Error(`HTTP ${response.status} for /curpairs`);
	}
	return parseCurrencyPairs(await response.json());
}
