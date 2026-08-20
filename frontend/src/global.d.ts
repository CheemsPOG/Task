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
