/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Shared FX types for the header ticker and datafeed bid_ask.
 *
 * CurrencyPair matches GET /curpairs (Java CurrencyPairDto).
 * RealtimeQuote matches Redis/WS ticks from Java QuoteBus
 * (field name rateMiliSecondUTC is the spec typo — keep it).
 */

export type PriceMode = 'bid' | 'ask' | 'mid';

export interface CurrencyPair {
	curpairCd: number;
	curpairName: string;
	curpairDisplay: string;
}

export interface RealtimeQuote {
	curpairCd: string;
	rateMiliSecondUTC: number;
	bid: number;
	ask: number;
	mid: number;
	high: number;
	low: number;
}

export function formatQuotePrice(pair: CurrencyPair | undefined, value: number): string {
	const scale = pair?.curpairName.endsWith('JPY') ? 3 : 5;
	return value.toFixed(scale);
}
