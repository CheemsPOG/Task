import Datafeed from './datafeed/datafeed.js';
import { cssBlobUrl, getChartOverrides, theme } from './theme.js';
import { installThemeToolbar } from './toolbar.js';

function initChart() {
	const Widget = window.TradingView?.widget;
	if (!Widget) {
		throw new Error('TradingView Advanced Charts library failed to load.');
	}

	const widget = new Widget({
		symbol: 'Binance:ETH/USDT',
		interval: '1D',
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
		],
		disabled_features: [
			'use_localstorage_for_settings',
			'save_chart_properties_to_local_storage',
			'volume_force_overlay',
		],
		overrides: getChartOverrides(theme),
	});

	window.tvWidget = widget;
	installThemeToolbar(widget);
}

window.addEventListener('DOMContentLoaded', initChart, { once: true });
