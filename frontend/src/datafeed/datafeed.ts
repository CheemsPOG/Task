import type {
	Bar,
	DatafeedConfiguration,
	ErrorCallback,
	HistoryCallback,
	IBasicDataFeed,
	LibrarySymbolInfo,
	OnReadyCallback,
	PeriodParams,
	ResolutionString,
	ResolveCallback,
	SearchSymbolsCallback,
	ServerTimeCallback,
	SubscribeBarsCallback,
} from 'charting_library';
import { apiGet } from '../api.ts';
import { subscribeOnStream, unsubscribeFromStream } from './streaming.ts';

const lastBarsCache = new Map<string, Bar>();

interface ServerTimeResponse {
	serverTime: number;
}

interface HistoryResponse {
	s?: string;
	noData?: boolean;
	bars?: Bar[];
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
			onResultReadyCallback(results);
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
			const data = await apiGet<HistoryResponse>('/history', {
				symbol: symbolInfo.ticker,
				resolution,
				from: periodParams.from,
				to: periodParams.to,
				countBack: periodParams.countBack,
			});

			if (data.s !== 'ok' || data.noData || !data.bars?.length) {
				onHistoryCallback([], { noData: true });
				return;
			}

			if (periodParams.firstDataRequest) {
				lastBarsCache.set(
					symbolInfo.ticker ?? symbolInfo.name,
					data.bars[data.bars.length - 1]
				);
			}

			onHistoryCallback(data.bars, { noData: false });
		} catch (error) {
			console.error('[getBars]', error);
			onErrorCallback(error instanceof Error ? error.message : String(error));
		}
	},

	subscribeBars(
		symbolInfo: LibrarySymbolInfo,
		resolution: ResolutionString,
		onRealtimeCallback: SubscribeBarsCallback,
		subscriberUID: string,
		onResetCacheNeededCallback: () => void
	) {
		subscribeOnStream(
			symbolInfo,
			resolution,
			onRealtimeCallback,
			subscriberUID,
			onResetCacheNeededCallback
		);
	},

	unsubscribeBars(subscriberUID: string) {
		unsubscribeFromStream(subscriberUID);
	},
};

export default Datafeed;
