/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

import { fetchAuthenticatedJson } from '../api.ts';
import { parseCurrencyPairs } from './quoteStore.ts';
import type { CurrencyPair } from './types.ts';

export async function fetchCurpairs(): Promise<CurrencyPair[]> {
	const payload = await fetchAuthenticatedJson<unknown>('/curpairs');
	return parseCurrencyPairs(payload);
}
