import type { DropdownItem, IChartingLibraryWidget, IDropdownApi } from 'charting_library';
import { resubscribeAllWithCurrentPrice } from '../datafeed/streaming.ts';
import { fetchCurpairs } from './currencyPairs.ts';
import { connectFxQuotes } from './fxQuotesSocket.ts';
import { quoteStore } from './quoteStore.ts';
import type { PriceMode } from './types.ts';

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

function syncQuoteToChartSymbol(widget: IChartingLibraryWidget): void {
	try {
		quoteStore.selectBySymbol(widget.activeChart().symbol());
	} catch {
		// Chart API is only available after onChartReady.
	}
}

function reloadChartSeries(widget: IChartingLibraryWidget): void {
	resubscribeAllWithCurrentPrice();
	try {
		widget.activeChart().resetData();
	} catch {
		// Chart API is only available after onChartReady.
	}
}

export function installFxQuoteToolbar(widget: IChartingLibraryWidget): void {
	widget.onChartReady(() => {
		syncQuoteToChartSymbol(widget);
		widget.activeChart().onSymbolChanged().subscribe(null, () => {
			syncQuoteToChartSymbol(widget);
		});
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

		const modeDropdown = await widget.createDropdown({
			title: modeLabel(quoteStore.mode),
			tooltip: 'Quote type: BID, ASK, or MID',
			align: 'left',
			items: modeItems(),
		});

		let lastMode = quoteStore.mode;

		function render(modeApi: IDropdownApi): void {
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
