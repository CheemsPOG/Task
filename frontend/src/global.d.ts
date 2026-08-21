/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
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
