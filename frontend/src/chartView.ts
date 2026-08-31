/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Last-viewed pair/interval restore and the visible window that matches
 * demo seed depth (see ChartCacheWriter.seedDepth). Header interval changes
 * keep the previous date range unless we snap it here.
 *
 * TradingView's onIntervalChanged hook only reliably accepts `period-back`
 * (`1D`, `12M`, …), not a unix `time-range`.
 */

import type {
	IChartingLibraryWidget,
	ResolutionString,
	TimeFrameValue,
} from 'charting_library';

/**
 * Period-back string for a header resolution so the window covers seeded bars.
 *
 * @param resolution widget resolution, e.g. `5` or `1D`
 * @returns TradingView period-back value such as `1D` or `12M`
 */
function periodBackForResolution(resolution: string): string {
	switch (resolution) {
		case '1S':
		case '1':
		case '5':
			return '1D';
		case '10':
		case '15':
		case '30':
			return '5D';
		case '60':
		case '120':
			return '1M';
		case '240':
		case '480':
			return '3M';
		case '1D':
			return '12M';
		case '1W':
			return '48M';
		case '1M':
			return '120M';
		default:
			return '1M';
	}
}

/**
 * Date range to apply when the header interval changes and TradingView
 * has not already set a time-frame (bottom toolbar click).
 *
 * @param resolution widget resolution
 * @returns `period-back` covering seeded history
 */
export function visibleTimeframeForResolution(resolution: string): TimeFrameValue {
	return {
		type: 'period-back',
		value: periodBackForResolution(resolution),
	} as TimeFrameValue;
}

/**
 * Applies the last viewed pair and interval if the chart differs.
 *
 * @param widget ready chart widget
 * @param symbol last viewed symbol
 * @param interval last viewed resolution
 */
export function restoreLastViewedPair(
	widget: IChartingLibraryWidget,
	symbol: string,
	interval: ResolutionString
): void {
	try {
		const chart = widget.activeChart();
		if (chart.symbol() === symbol && String(chart.resolution()) === String(interval)) {
			return;
		}
		widget.setSymbol(symbol, interval, () => undefined);
	} catch {
		// Chart API is only available after onChartReady.
	}
}
