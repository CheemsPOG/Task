/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Loads the FX pair catalog for the quote toolbar.
 *
 * GET /curpairs (JWT required). Same master rows as docs 123/124 (m_ccypairs),
 * but the DTO uses numeric curpairCd = priority. Not a 120–139 design doc.
 */

import { fetchAuthenticatedJson } from '../api.ts';
import { parseCurrencyPairs } from './quoteStore.ts';
import type { CurrencyPair } from './types.ts';

export async function fetchCurpairs(): Promise<CurrencyPair[]> {
	const payload = await fetchAuthenticatedJson<unknown>('/curpairs');
	return parseCurrencyPairs(payload);
}
