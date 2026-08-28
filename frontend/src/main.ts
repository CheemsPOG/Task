/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Browser entry: login gate, then one TradingView Advanced Charts widget.
 *
 * Boot order:
 * 1. If a JWT is already in sessionStorage, or the HttpOnly refresh cookie
 *    can mint a new one, hide the overlay and create the widget.
 * 2. Otherwise show login (see login.ts). Demo users: demo/demo, demo2/demo2.
 *
 * The widget talks to Java REST through datafeed/datafeed.ts (docs 120–126).
 * Live candles go through datafeed/streaming.ts → Python /ws/stream.
 * Live BID/ASK/MID ticker is fx/quoteToolbar.ts → Python /ws/fx-quotes.
 *
 * Save/Load uses ServerSaveLoadAdapter (docs 127–139): layouts, study
 * templates (`study_templates`), and chart style templates
 * (`chart_template_storage`) persist in Postgres for the JWT customer.
 *
 * Vite proxies /api and /curpairs to Java :8080, /ws to Python :8081.
 */

import type { ChartingLibraryFeatureset, ResolutionString } from 'charting_library';
import { setUnauthorizedHandler } from './api.ts';
import { getToken, logout, refreshAccessToken } from './auth.ts';
import Datafeed from './datafeed/datafeed.ts';
import { installFxQuoteToolbar } from './fx/quoteToolbar.ts';
import {
	hideLoginOverlay,
	installLoginOverlay,
	showLoginOverlay,
} from './login.ts';
import { ServerSaveLoadAdapter } from './save-load-adapter.ts';
import { cssBlobUrl, getChartOverrides, theme } from './theme.ts';
import { installLogoutButton, installThemeToolbar } from './toolbar.ts';

let chartStarted = false;

/**
 * Creates the chart once. TradingView's constructor reads window.TradingView
 * from charting_library.standalone.js loaded in index.html.
 */
function initChart(): void {
	if (chartStarted) {
		return;
	}
	chartStarted = true;

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
			'study_templates',
			'chart_template_storage',
		] as unknown as ChartingLibraryFeatureset[],
		disabled_features: [
			'use_localstorage_for_settings',
			'save_chart_properties_to_local_storage',
			// Keep volume in its own pane so a low-volume forming bar cannot
			// stretch the price Y-axis down to zero.
			'volume_force_overlay',
		],
		save_load_adapter: new ServerSaveLoadAdapter(),
		overrides: getChartOverrides(theme),
	});

	window.tvWidget = widget;
	installThemeToolbar(widget);
	installFxQuoteToolbar(widget);
	installLogoutButton(widget, () => {
		void logout().finally(() => {
			window.location.reload();
		});
	});
}

/**
 * Auth first, chart second. A 401 from any later /api call triggers logout
 * via setUnauthorizedHandler in api.ts.
 */
async function boot(): Promise<void> {
	setUnauthorizedHandler(() => {
		void logout().finally(() => {
			window.location.reload();
		});
	});

	installLoginOverlay({
		onLoggedIn: () => {
			hideLoginOverlay();
			initChart();
		},
	});

	if (getToken()) {
		hideLoginOverlay();
		initChart();
		return;
	}

	try {
		await refreshAccessToken();
		hideLoginOverlay();
		initChart();
	} catch {
		showLoginOverlay();
	}
}

window.addEventListener('DOMContentLoaded', () => {
	void boot();
}, { once: true });
