/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Chart header: live BID/ASK/MID ticker + price-mode dropdown.
 *
 * On header ready: GET /curpairs, then open /ws/fx-quotes. Changing BID/ASK/MID
 * resets history and resubscribes /ws/stream so candles use the same side.
 *
 * Selected pair follows the widget symbol (USD/JPY ↔ curpairCd 1).
 */

import type {
	DropdownItem,
	IChartingLibraryWidget,
	IChartWidgetApi,
	IDropdownApi,
} from 'charting_library';
import { saveLastChartView } from '../chartPrefs.ts';
import { ensureStreamAlive, resubscribeAllWithCurrentPrice } from '../datafeed/streaming.ts';
import { fetchCurpairs } from './currencyPairs.ts';
import { connectFxQuotes } from './fxQuotesSocket.ts';
import { quoteStore } from './quoteStore.ts';
import { formatQuotePrice, type PriceMode } from './types.ts';

const MODES: PriceMode[] = ['bid', 'ask', 'mid'];

function modeLabel(mode: PriceMode): string {
	return mode.toUpperCase();
}

function modeItems(): DropdownItem[] {
	return MODES.map(mode => ({
		title: quoteStore.mode === mode ? `✓ ${modeLabel(mode)}` : modeLabel(mode),
		onSelect: () => quoteStore.setMode(mode),
	}));
}

function quoteLabel(): string {
	if (quoteStore.pairsError) {
		return 'Pairs unavailable';
	}
	const pair = quoteStore.pair();
	if (!pair) {
		return '—';
	}
	const quote = quoteStore.quote();
	if (!quote) {
		return `${pair.curpairDisplay} …`;
	}
	const bid = formatQuotePrice(pair, quote.bid);
	const ask = formatQuotePrice(pair, quote.ask);
	const mid = formatQuotePrice(pair, quote.mid);
	return `${pair.curpairDisplay}  BID ${bid}  ASK ${ask}  MID ${mid}`;
}

let boundChart: IChartWidgetApi | null = null;

function persistChartView(widget: IChartingLibraryWidget): void {
	try {
		const chart = widget.activeChart();
		saveLastChartView(chart.symbol(), String(chart.resolution()));
	} catch {
		// Chart API is only available after onChartReady.
	}
}

function syncQuoteToChartSymbol(widget: IChartingLibraryWidget): void {
	try {
		quoteStore.selectBySymbol(widget.activeChart().symbol());
	} catch {
		// Chart API is only available after onChartReady.
	}
}

function bindActiveChart(widget: IChartingLibraryWidget): void {
	try {
		const chart = widget.activeChart();
		if (boundChart === chart) {
			return;
		}
		boundChart = chart;
		chart.onSymbolChanged().subscribe(null, () => {
			syncQuoteToChartSymbol(widget);
			persistChartView(widget);
		});
		chart.onIntervalChanged().subscribe(null, () => {
			persistChartView(widget);
		});
	} catch {
		// Chart API is only available after onChartReady.
	}
}

function reloadChartSeries(widget: IChartingLibraryWidget): void {
	// Invalidate TV bar cache first, then resetData so getBars refetches.
	resubscribeAllWithCurrentPrice();
	try {
		widget.activeChart().resetData();
	} catch {
		// Chart API is only available after onChartReady.
	}
}

export function installFxQuoteToolbar(widget: IChartingLibraryWidget): void {
	widget.onChartReady(() => {
		const onChartLoaded = (): void => {
			bindActiveChart(widget);
			syncQuoteToChartSymbol(widget);
			persistChartView(widget);
			ensureStreamAlive();
		};
		onChartLoaded();
		widget.subscribe('chart_loaded', onChartLoaded);
	});

	widget.headerReady().then(async () => {
		try {
			const pairs = await fetchCurpairs();
			if (pairs.length === 0) {
				quoteStore.markPairsError();
			} else {
				quoteStore.setPairs(pairs);
			}
		} catch (error) {
			console.error('[fx] /curpairs unavailable', error);
			quoteStore.markPairsError();
		}

		syncQuoteToChartSymbol(widget);
		connectFxQuotes();

		const quoteEl = widget.createButton({
			useTradingViewStyle: false,
			align: 'left',
		});
		if (quoteEl) {
			quoteEl.id = 'fx-quote-ticker';
			quoteEl.style.padding = '0 10px';
			quoteEl.style.pointerEvents = 'none';
		}

		const modeDropdown = await widget.createDropdown({
			title: modeLabel(quoteStore.mode),
			tooltip: 'Quote type: BID, ASK, or MID',
			align: 'left',
			items: modeItems(),
		});

		let lastMode = quoteStore.mode;

		function render(modeApi: IDropdownApi): void {
			if (quoteEl) {
				const label = quoteLabel();
				quoteEl.textContent = label;
				quoteEl.title = label;
			}
			if (quoteStore.mode === lastMode) {
				return;
			}
			lastMode = quoteStore.mode;
			modeApi.applyOptions({
				title: modeLabel(quoteStore.mode),
				items: modeItems(),
			});
			reloadChartSeries(widget);
		}

		quoteStore.subscribe(() => render(modeDropdown));
		render(modeDropdown);
	});
}
