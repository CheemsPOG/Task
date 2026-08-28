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
 * Price mode, theme, and last viewed pair persist in localStorage
 * (`chartPrefs.ts`). Named layouts win on refresh (`load_last_chart`).
 * A local draft is used only when the customer has no saved layouts.
 * USD/JPY is only the first-visit default.
 *
 * Vite proxies /api and /curpairs to Java :8080, /ws to Python :8081.
 */

import type { ChartingLibraryFeatureset, IChartingLibraryWidget, ResolutionString } from 'charting_library';
import { apiGet, setUnauthorizedHandler } from './api.ts';
import { getToken, logout, refreshAccessToken } from './auth.ts';
import {
	DEFAULT_INTERVAL,
	DEFAULT_SYMBOL,
	loadChartDraft,
	loadChartPrefs,
} from './chartPrefs.ts';
import { snapshotChart } from './chartReset.ts';
import Datafeed from './datafeed/datafeed.ts';
import { quoteStore } from './fx/quoteStore.ts';
import { installFxQuoteToolbar } from './fx/quoteToolbar.ts';
import {
	hideLoginOverlay,
	installLoginOverlay,
	showLoginOverlay,
} from './login.ts';
import { ServerSaveLoadAdapter, type LayoutListItem } from './save-load-adapter.ts';
import { cssBlobUrl, getChartOverrides, resolveTheme } from './theme.ts';
import { installLogoutButton, installThemeToolbar } from './toolbar.ts';

let chartStarted = false;

/**
 * Creates the chart once. TradingView's constructor reads window.TradingView
 * from charting_library.standalone.js loaded in index.html.
 */
async function initChart(): Promise<void> {
	if (chartStarted) {
		return;
	}
	chartStarted = true;

	const storedPrefs = loadChartPrefs();
	if (storedPrefs.priceMode) {
		quoteStore.setMode(storedPrefs.priceMode);
	}

	const currentTheme = resolveTheme();
	const bootSymbol = storedPrefs.lastSymbol ?? DEFAULT_SYMBOL;
	const bootInterval = (storedPrefs.lastInterval ?? DEFAULT_INTERVAL) as ResolutionString;

	let hasLayouts = false;
	try {
		const layouts = await apiGet<LayoutListItem[]>('/layouts');
		hasLayouts = layouts.length > 0;
	} catch {
		hasLayouts = false;
	}
	const draft = loadChartDraft() ?? undefined;

	const Widget = window.TradingView?.widget;
	if (!Widget) {
		throw new Error('TradingView Advanced Charts library failed to load.');
	}

	let widget!: IChartingLibraryWidget;
	const adapter = new ServerSaveLoadAdapter({
		onLastLayoutRemoved: () => {
			try {
				widget.closePopupsAndDialogs();
			} catch {
				// Load dialog may already be gone.
			}
		},
	});

	widget = new Widget({
		symbol: bootSymbol,
		interval: bootInterval,
		fullscreen: true,
		container: 'tv_chart_container',
		datafeed: Datafeed,
		library_path: '/charting_library/',
		locale: 'en',
		timezone: 'Etc/UTC',
		symbol_search_request_delay: 400,
		theme: currentTheme,
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
		save_load_adapter: adapter,
		load_last_chart: hasLayouts,
		saved_data: hasLayouts ? undefined : draft,
		auto_save_delay: 5,
		overrides: getChartOverrides(currentTheme),
	});

	window.tvWidget = widget;
	installThemeToolbar(widget);
	installFxQuoteToolbar(widget);
	installLogoutButton(widget, () => {
		void logout().finally(() => {
			window.location.reload();
		});
	});

	widget.onChartReady(() => {
		if (!hasLayouts) {
			restoreLastViewedPair(widget, bootSymbol, bootInterval);
		}
		const captureFactory = !hasLayouts && !draft;
		snapshotChart(widget, captureFactory);
		widget.subscribe('onAutoSaveNeeded', () => {
			snapshotChart(widget, false);
		});
	});
}

function restoreLastViewedPair(
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
			void initChart();
		},
	});

	if (getToken()) {
		hideLoginOverlay();
		void initChart();
		return;
	}

	try {
		await refreshAccessToken();
		hideLoginOverlay();
		void initChart();
	} catch {
		showLoginOverlay();
	}
}

window.addEventListener('DOMContentLoaded', () => {
	void boot();
}, { once: true });
