/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Factory chart (USD/JPY 1D + volume) used when the last named layout is
 * deleted — the layout Load dialog has no built-in “Apply default”.
 */

import type { IChartingLibraryWidget, ResolutionString } from 'charting_library';
import {
	DEFAULT_INTERVAL,
	DEFAULT_SYMBOL,
	loadFactoryChart,
	saveChartDraft,
	saveFactoryChart,
} from './chartPrefs.ts';
import { getChartOverrides, resolveTheme } from './theme.ts';

/**
 * Captures a clean constructor chart once, then always writes the session draft.
 *
 * @param widget ready chart widget
 * @param captureFactory when true, store as the shared factory snapshot if missing
 */
export function snapshotChart(
	widget: IChartingLibraryWidget,
	captureFactory: boolean
): void {
	try {
		widget.save(state => {
			if (captureFactory) {
				saveFactoryChart(state);
			}
			saveChartDraft(state);
		});
	} catch {
		// Chart API is only available after onChartReady.
	}
}

/**
 * Loads the factory snapshot (or rebuilds USD/JPY 1D) and closes TV dialogs.
 *
 * @param widget chart widget
 */
export function applyDefaultChart(widget: IChartingLibraryWidget): void {
	try {
		widget.closePopupsAndDialogs();
	} catch {
		// Dialog API may be unavailable while the Load UI is tearing down.
	}

	const factory = loadFactoryChart();
	if (factory) {
		try {
			widget.load(factory, { uid: 0, name: '', description: '' });
			saveChartDraft(factory);
			reapplyTheme(widget);
			return;
		} catch {
			// Fall through to symbol reset.
		}
	}

	applyDefaultChartFallback(widget);
}

function reapplyTheme(widget: IChartingLibraryWidget): void {
	const themeName = resolveTheme();
	void widget
		.changeTheme(themeName, { disableUndo: true })
		.then(() => {
			widget.applyOverrides(getChartOverrides(themeName));
		})
		.catch(() => {
			widget.applyOverrides(getChartOverrides(themeName));
		});
}

function applyDefaultChartFallback(widget: IChartingLibraryWidget): void {
	try {
		const chart = widget.activeChart();
		chart.removeAllShapes();
		chart.removeAllStudies();
		widget.setSymbol(DEFAULT_SYMBOL, DEFAULT_INTERVAL as ResolutionString, () => {
			try {
				widget.clearUndoHistory();
			} catch {
				/* ignore */
			}
			snapshotChart(widget, true);
		});
		reapplyTheme(widget);
	} catch {
		// Chart API is only available after onChartReady.
	}
}
