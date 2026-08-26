/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
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
import { cssBlobUrl, getChartOverrides, theme } from './theme.ts';
import { installLogoutButton, installThemeToolbar } from './toolbar.ts';

let chartStarted = false;

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
	installLogoutButton(widget, () => {
		void logout().finally(() => {
			window.location.reload();
		});
	});
}

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
