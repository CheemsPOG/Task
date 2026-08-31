/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Captures widget.save() snapshots: one shared factory chart (first visit) and
 * the per-session draft used when the customer has no named layouts.
 */

import type { IChartingLibraryWidget } from 'charting_library';
import { saveChartDraft, saveFactoryChart } from './chartPrefs.ts';

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
