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
 * opens Python /ws/stream. History uses `bars[]` (time in ms). Columnar t[]
 * from the same JSON is unix seconds and is ignored here.
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

interface ServerTimeResponse {
	serverTime: number;
}

interface HistoryResponse {
	s?: string;
	noData?: boolean;
	bars?: Bar[];
	nextTime?: number;
	errmsg?: string;
	t?: number[];
	o?: number[];
	h?: number[];
	l?: number[];
	c?: number[];
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

			if (data.s !== 'ok' || data.noData || !data.bars?.length) {
				// Doc 121 / UDF: nextTime is unix seconds; library bar times are ms.
				const meta: { noData: true; nextTime?: number } = { noData: true };
				if (typeof data.nextTime === 'number' && Number.isFinite(data.nextTime)) {
					meta.nextTime = data.nextTime * 1000;
				}
				onHistoryCallback([], meta);
				return;
			}

			const bars = data.bars;
			if (periodParams.firstDataRequest) {
				lastBarsCache.set(
					`${symbolInfo.ticker ?? symbolInfo.name}|${quoteStore.mode}`,
					bars[bars.length - 1]
				);
			}

			onHistoryCallback(bars, { noData: false });
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
		const cacheKey = `${symbolInfo.ticker ?? symbolInfo.name}|${quoteStore.mode}`;
		const lastBar = lastBarsCache.get(cacheKey);
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
