import type { CurrencyPair, PriceMode, RealtimeQuote } from './types.ts';

export type QuoteStoreListener = () => void;

function isFiniteNumber(value: unknown): value is number {
	return typeof value === 'number' && Number.isFinite(value);
}

export function parseCurrencyPairs(payload: unknown): CurrencyPair[] {
	if (!Array.isArray(payload)) {
		return [];
	}

	const pairs: CurrencyPair[] = [];
	const seen = new Set<string>();
	for (const item of payload) {
		if (!item || typeof item !== 'object') {
			continue;
		}
		const row = item as Record<string, unknown>;
		const curpairCd = Number(row.curpairCd);
		const curpairName = typeof row.curpairName === 'string' ? row.curpairName : '';
		const curpairDisplay =
			typeof row.curpairDisplay === 'string' ? row.curpairDisplay : '';
		if (!Number.isInteger(curpairCd) || !curpairName || !curpairDisplay) {
			continue;
		}
		const key = String(curpairCd);
		if (seen.has(key)) {
			continue;
		}
		seen.add(key);
		pairs.push({ curpairCd, curpairName, curpairDisplay });
	}
	return pairs;
}

export function parseQuote(payload: unknown): RealtimeQuote | null {
	if (!payload || typeof payload !== 'object') {
		return null;
	}
	const row = payload as Record<string, unknown>;
	const curpairCd = row.curpairCd == null ? '' : String(row.curpairCd).trim();
	const rateMiliSecondUTC = Number(row.rateMiliSecondUTC);
	const bid = Number(row.bid);
	const ask = Number(row.ask);
	const mid = Number(row.mid);
	const high = Number(row.high);
	const low = Number(row.low);

	if (
		!curpairCd ||
		!isFiniteNumber(rateMiliSecondUTC) ||
		!isFiniteNumber(bid) ||
		!isFiniteNumber(ask) ||
		!isFiniteNumber(mid) ||
		!isFiniteNumber(high) ||
		!isFiniteNumber(low)
	) {
		return null;
	}

	return { curpairCd, rateMiliSecondUTC, bid, ask, mid, high, low };
}

class QuoteStore {
	pairs: CurrencyPair[] = [];
	private pairsByCd = new Map<string, CurrencyPair>();
	private quotes = new Map<string, RealtimeQuote>();
	private pendingSymbol: string | null = null;
	selectedCd: string | null = null;
	mode: PriceMode = 'bid';
	pairsError = false;
	private listeners = new Set<QuoteStoreListener>();

	subscribe(listener: QuoteStoreListener): () => void {
		this.listeners.add(listener);
		return () => this.listeners.delete(listener);
	}

	setPairs(pairs: CurrencyPair[]): void {
		this.pairs = pairs;
		this.pairsByCd = new Map(pairs.map(pair => [String(pair.curpairCd), pair]));
		this.pairsError = false;
		if (this.pendingSymbol) {
			this.applySymbol(this.pendingSymbol);
			return;
		}
		if (!this.selectedCd || !this.pairsByCd.has(this.selectedCd)) {
			this.selectedCd = pairs[0] ? String(pairs[0].curpairCd) : null;
		}
		this.notify();
	}

	markPairsError(): void {
		this.pairsError = true;
		this.notify();
	}

	setMode(mode: PriceMode): void {
		if (this.mode === mode) {
			return;
		}
		this.mode = mode;
		this.notify();
	}

	selectBySymbol(symbol: string): void {
		this.pendingSymbol = symbol;
		if (this.pairs.length === 0) {
			return;
		}
		this.applySymbol(symbol);
	}

	applyQuote(payload: unknown): void {
		const quote = parseQuote(payload);
		if (!quote) {
			console.warn('[fx] ignored malformed quote', payload);
			return;
		}
		if (!this.pairsByCd.has(quote.curpairCd)) {
			console.warn('[fx] unknown curpairCd', quote.curpairCd);
			return;
		}
		this.quotes.set(quote.curpairCd, quote);
		if (quote.curpairCd === this.selectedCd) {
			this.notify();
		}
	}

	pair(curpairCd: string | null = this.selectedCd): CurrencyPair | undefined {
		if (!curpairCd) {
			return undefined;
		}
		return this.pairsByCd.get(curpairCd);
	}

	quote(curpairCd: string | null = this.selectedCd): RealtimeQuote | undefined {
		if (!curpairCd) {
			return undefined;
		}
		return this.quotes.get(curpairCd);
	}

	private applySymbol(symbol: string): void {
		const pair = matchPair(this.pairs, symbol);
		const nextCd = pair ? String(pair.curpairCd) : null;
		if (this.selectedCd === nextCd) {
			return;
		}
		this.selectedCd = nextCd;
		this.notify();
	}

	private notify(): void {
		this.listeners.forEach(listener => listener());
	}
}

export function matchPair(pairs: CurrencyPair[], symbol: string): CurrencyPair | undefined {
	const needle = symbol.trim().toLowerCase();
	const stripped = needle.startsWith('fx:') ? needle.slice(3) : needle;
	return pairs.find(
		pair =>
			pair.curpairDisplay.toLowerCase() === stripped
			|| pair.curpairName.toLowerCase() === stripped
			|| String(pair.curpairCd) === stripped
	);
}

export const quoteStore = new QuoteStore();
