import type { DropdownItem, IChartingLibraryWidget, IDropdownApi } from 'charting_library';
import { fetchCurpairs } from './currencyPairs.ts';
import { connectFxQuotes } from './fxQuotesSocket.ts';
import { quoteStore } from './quoteStore.ts';
import { formatQuotePrice, selectedPrice, type PriceMode } from './types.ts';

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
			items: MODES.map(mode => ({
				title: modeLabel(mode),
				onSelect: () => undefined,
			})),
		});

		modeDropdown.applyOptions({
			title: modeLabel(quoteStore.mode),
			items: modeItems(),
		});

		const priceEl = widget.createButton({
			useTradingViewStyle: false,
			align: 'left',
		});
		if (!priceEl) {
			return;
		}
		priceEl.dataset.internalAllowKeyboardNavigation = 'true';
		priceEl.id = 'fx-quote-price';
		priceEl.title = 'Selected FX quote';

		let lastMode = quoteStore.mode;
		let lastPriceText = '';

		function render(modeApi: IDropdownApi): void {
			const pair = quoteStore.pair();
			const quote = quoteStore.quote();
			const priceText = quote
				? formatQuotePrice(pair, selectedPrice(quote, quoteStore.mode))
				: '—';

			if (priceText !== lastPriceText) {
				priceEl.textContent = priceText;
				lastPriceText = priceText;
			}
			priceEl.title = pair
				? `${pair.curpairDisplay} ${modeLabel(quoteStore.mode)}`
				: 'Selected FX quote';

			if (quoteStore.mode !== lastMode) {
				lastMode = quoteStore.mode;
				modeApi.applyOptions({
					title: modeLabel(quoteStore.mode),
					items: modeItems(),
				});
			}
		}

		quoteStore.subscribe(() => render(modeDropdown));
		render(modeDropdown);
	});
}
