import { apiGet } from '../api.js';
import { subscribeOnStream, unsubscribeFromStream } from './streaming.js';

const lastBarsCache = new Map();

export default {
	onReady(callback) {
		apiGet('/config')
			.then(config => callback(config))
			.catch(error => {
				console.error('[onReady]', error);
				callback({
					supported_resolutions: ['1', '5', '15', '60', '1D'],
					supports_time: true,
				});
			});
	},

	getServerTime(callback) {
		apiGet('/time')
			.then(({ serverTime }) => {
				callback(
					Number.isFinite(serverTime)
						? serverTime
						: Math.floor(Date.now() / 1000)
				);
			})
			.catch(() => callback(Math.floor(Date.now() / 1000)));
	},

	async searchSymbols(userInput, exchange, symbolType, onResultReadyCallback) {
		try {
			const results = await apiGet('/search', {
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
		symbolName,
		onSymbolResolvedCallback,
		onResolveErrorCallback
	) {
		try {
			const symbolInfo = await apiGet('/symbols', { symbol: symbolName });
			onSymbolResolvedCallback(symbolInfo);
		} catch (error) {
			console.warn('[resolveSymbol]', symbolName, error);
			onResolveErrorCallback('unknown_symbol');
		}
	},

	async getBars(
		symbolInfo,
		resolution,
		periodParams,
		onHistoryCallback,
		onErrorCallback
	) {
		try {
			const data = await apiGet('/history', {
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
				lastBarsCache.set(symbolInfo.ticker, data.bars[data.bars.length - 1]);
			}

			onHistoryCallback(data.bars, { noData: false });
		} catch (error) {
			console.error('[getBars]', error);
			onErrorCallback(error);
		}
	},

	subscribeBars(
		symbolInfo,
		resolution,
		onRealtimeCallback,
		subscriberUID,
		onResetCacheNeededCallback
	) {
		subscribeOnStream(
			symbolInfo,
			resolution,
			onRealtimeCallback,
			subscriberUID,
			onResetCacheNeededCallback
		);
	},

	unsubscribeBars(subscriberUID) {
		unsubscribeFromStream(subscriberUID);
	},

	async getMarks(symbolInfo, from, to, onDataCallback, resolution) {
		try {
			const marks = await apiGet('/marks', {
				symbol: symbolInfo.ticker,
				from,
				to,
				resolution,
			});
			onDataCallback(marks);
		} catch (error) {
			console.warn('[getMarks]', error);
			onDataCallback([]);
		}
	},

	async getTimescaleMarks(symbolInfo, from, to, onDataCallback, resolution) {
		try {
			const marks = await apiGet('/timescale-marks', {
				symbol: symbolInfo.ticker,
				from,
				to,
				resolution,
			});
			onDataCallback(marks);
		} catch (error) {
			console.warn('[getTimescaleMarks]', error);
			onDataCallback([]);
		}
	},
};
