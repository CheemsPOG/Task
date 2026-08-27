/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Ambient Window types for the TradingView UMD build.
 *
 * index.html loads charting_library.standalone.js, which sets window.TradingView.
 * main.ts stores the widget instance on window.tvWidget for the header add-ons.
 */

import type {
	ChartingLibraryWidgetConstructor,
	IChartingLibraryWidget,
} from 'charting_library';

declare global {
	interface Window {
		TradingView?: {
			widget: ChartingLibraryWidgetConstructor;
		};
		tvWidget?: IChartingLibraryWidget;
	}
}

export {};
