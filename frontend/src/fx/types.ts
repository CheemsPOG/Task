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

export function selectedPrice(quote: RealtimeQuote, mode: PriceMode): number {
	if (mode === 'ask') {
		return quote.ask;
	}
	if (mode === 'mid') {
		return quote.mid;
	}
	return quote.bid;
}

export function formatQuotePrice(pair: CurrencyPair | undefined, value: number): string {
	const scale = pair?.curpairName.endsWith('JPY') ? 3 : 5;
	return value.toFixed(scale);
}
