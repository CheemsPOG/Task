/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * TradingView IBasicDataFeed → Java REST (design docs 120–126).
 *
 * Widget callback → HTTP:
 * - onReady            GET /api/config              (120)
 * - getBars            GET /api/history             (121)
 * - getServerTime      GET /api/time                (122)
 * - resolveSymbol      GET /api/symbols             (123)
 * - searchSymbols      GET /api/search              (124)
 * - getMarks           GET /api/marks               (125)
 * - getTimescaleMarks  GET /api/timescale_marks     (126)
 *
 * Live updates are not REST. subscribeBars hands off to streaming.ts, which
 * opens Python /ws/stream. History JSON is doc 121 columnar (`s`, `t`, `o`,
 * `h`, `l`, `c`, `nextTime`); this file zips those arrays into widget `Bar[]`
 * (`time` in milliseconds).
 *
 * bid_ask on history follows quoteStore.mode (BID/ASK/MID toolbar).
 */

import type {
	Bar,
	DatafeedConfiguration,
	ErrorCallback,
	GetMarksCallback,
	HistoryCallback,
	IBasicDataFeed,
	LibrarySymbolInfo,
	Mark,
	OnReadyCallback,
	PeriodParams,
	ResolutionString,
	ResolveCallback,
	SearchSymbolsCallback,
	ServerTimeCallback,
	SubscribeBarsCallback,
	TimescaleMark,
} from 'charting_library';
import { apiGet } from '../api.ts';
import { quoteStore } from '../fx/quoteStore.ts';
import { subscribeOnStream, unsubscribeFromStream } from './streaming.ts';

const lastBarsCache = new Map<string, Bar>();

function barCacheKey(
	symbolInfo: LibrarySymbolInfo,
	resolution: ResolutionString
): string {
	return `${symbolInfo.ticker ?? symbolInfo.name}|${resolution}|${quoteStore.mode}`;
}

interface ServerTimeResponse {
	serverTime: number;
}

interface HistoryResponse {
	s?: 'ok' | 'no_data';
	t?: number[];
	o?: number[];
	h?: number[];
	l?: number[];
	c?: number[];
	nextTime?: number;
}


const Datafeed: IBasicDataFeed = {
	onReady(callback: OnReadyCallback) {
		apiGet<DatafeedConfiguration>('/config')
			.then(config => callback(config))
			.catch(error => {
				console.error('[onReady]', error);
				callback({
					supported_resolutions: ['1', '5', '15', '60', '1D'] as ResolutionString[],
					supports_time: true,
				});
			});
	},

	getServerTime(callback: ServerTimeCallback) {
		apiGet<ServerTimeResponse>('/time')
			.then(({ serverTime }) => {
				callback(
					Number.isFinite(serverTime)
						? serverTime
						: Math.floor(Date.now() / 1000)
				);
			})
			.catch(() => callback(Math.floor(Date.now() / 1000)));
	},

	async searchSymbols(
		userInput: string,
		exchange: string,
		symbolType: string,
		onResultReadyCallback: SearchSymbolsCallback
	) {
		try {
			const results = await apiGet<Parameters<SearchSymbolsCallback>[0]>('/search', {
				query: userInput,
				exchange,
				type: symbolType,
				limit: 50,
			});
			// Library SYMBOL column uses `symbol`; API keeps ccypair_cd there — show slash form via ticker.
			onResultReadyCallback(
				results.map(item => ({
					...item,
					symbol: item.ticker || item.full_name || item.symbol,
				}))
			);
		} catch (error) {
			console.error('[searchSymbols]', error);
			onResultReadyCallback([]);
		}
	},

	async resolveSymbol(
		symbolName: string,
		onSymbolResolvedCallback: ResolveCallback,
		onResolveErrorCallback: ErrorCallback
	) {
		try {
			const symbolInfo = await apiGet<LibrarySymbolInfo>('/symbols', {
				symbol: symbolName,
			});
			onSymbolResolvedCallback(symbolInfo);
		} catch (error) {
			console.warn('[resolveSymbol]', symbolName, error);
			onResolveErrorCallback('unknown_symbol');
		}
	},

	async getBars(
		symbolInfo: LibrarySymbolInfo,
		resolution: ResolutionString,
		periodParams: PeriodParams,
		onHistoryCallback: HistoryCallback,
		onErrorCallback: ErrorCallback
	) {
		try {
			const symbolCd = (symbolInfo.ticker ?? symbolInfo.name ?? '')
				.replace(/[^A-Za-z]/g, '')
				.toUpperCase();
			const bidAsk = quoteStore.mode.toUpperCase();
			const data = await apiGet<HistoryResponse>('/history', {
				symbol: symbolCd || symbolInfo.ticker,
				resolution,
				from: periodParams.from,
				to: periodParams.to,
				countBack: periodParams.countBack,
				bid_ask: bidAsk,
			});

			const times = data.t;
			const opens = data.o;
			const highs = data.h;
			const lows = data.l;
			const closes = data.c;
			if (data.s === 'ok' && times && times.length > 0 && opens && highs && lows && closes) {
				const bars: Bar[] = times.map((timestamp, index) => ({
					time: timestamp * 1000,
					open: opens[index],
					high: highs[index],
					low: lows[index],
					close: closes[index],
				}));
				if (periodParams.firstDataRequest) {
					lastBarsCache.set(barCacheKey(symbolInfo, resolution), bars[bars.length - 1]);
				}
				onHistoryCallback(bars, { noData: false });
				return;
			}

			// Doc 121: nextTime is unix seconds; library bar times are ms.
			const meta: { noData: true; nextTime?: number } = { noData: true };
			if (typeof data.nextTime === 'number' && Number.isFinite(data.nextTime)) {
				meta.nextTime = data.nextTime * 1000;
			}
			onHistoryCallback([], meta);
		} catch (error) {
			console.error('[getBars]', error);
			onErrorCallback(error instanceof Error ? error.message : String(error));
		}
	},

	getMarks(
		symbolInfo: LibrarySymbolInfo,
		from: number,
		to: number,
		onDataCallback: GetMarksCallback<Mark>,
		resolution: ResolutionString
	) {
		apiGet<Mark[]>('/marks', {
			symbol: symbolInfo.ticker ?? symbolInfo.name,
			resolution,
			from,
			to,
		})
			.then(marks => onDataCallback(Array.isArray(marks) ? marks : []))
			.catch(error => {
				console.error('[getMarks]', error);
				onDataCallback([]);
			});
	},

	getTimescaleMarks(
		symbolInfo: LibrarySymbolInfo,
		from: number,
		to: number,
		onDataCallback: GetMarksCallback<TimescaleMark>,
		resolution: ResolutionString
	) {
		apiGet<TimescaleMark[]>('/timescale_marks', {
			symbol: symbolInfo.ticker ?? symbolInfo.name,
			resolution,
			from,
			to,
		})
			.then(marks => onDataCallback(Array.isArray(marks) ? marks : []))
			.catch(error => {
				console.error('[getTimescaleMarks]', error);
				onDataCallback([]);
			});
	},

	subscribeBars(
		symbolInfo: LibrarySymbolInfo,
		resolution: ResolutionString,
		onRealtimeCallback: SubscribeBarsCallback,
		subscriberUID: string,
		onResetCacheNeededCallback: () => void
	) {
		const lastBar = lastBarsCache.get(barCacheKey(symbolInfo, resolution));
		subscribeOnStream(
			symbolInfo,
			resolution,
			onRealtimeCallback,
			subscriberUID,
			onResetCacheNeededCallback,
			lastBar?.time ?? 0
		);
	},

	unsubscribeBars(subscriberUID: string) {
		unsubscribeFromStream(subscriberUID);
	},
};

export default Datafeed;
