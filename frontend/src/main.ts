import type { ChartingLibraryFeatureset, ResolutionString } from 'charting_library';
import Datafeed from './datafeed/datafeed.ts';
import { installFxQuoteToolbar } from './fx/quoteToolbar.ts';
import { cssBlobUrl, getChartOverrides, theme } from './theme.ts';
import { installThemeToolbar } from './toolbar.ts';

function initChart(): void {
	const Widget = window.TradingView?.widget;
	if (!Widget) {
		throw new Error('TradingView Advanced Charts library failed to load.');
	}

	const widget = new Widget({
		symbol: 'USD/JPY',
		interval: '1D' as ResolutionString,
		fullscreen: true,
		container: 'tv_chart_container',
		datafeed: Datafeed,
		library_path: '/charting_library/',
		locale: 'en',
		timezone: 'Etc/UTC',
		symbol_search_request_delay: 400,
		theme,
		custom_css_url: cssBlobUrl,
		enabled_features: [
			'seconds_resolution',
			'custom_resolutions',
			'allow_arbitrary_symbol_search_input',
		] as unknown as ChartingLibraryFeatureset[],
		disabled_features: [
			'use_localstorage_for_settings',
			'save_chart_properties_to_local_storage',
			'volume_force_overlay',
		],
		overrides: getChartOverrides(theme),
	});

	window.tvWidget = widget;
	installThemeToolbar(widget);
	installFxQuoteToolbar(widget);
}

window.addEventListener('DOMContentLoaded', initChart, { once: true });
