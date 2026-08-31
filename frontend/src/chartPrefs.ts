/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Per-customer browser prefs (price mode, theme) and unsaved chart draft.
 *
 * Keys are scoped by JWT `customer_no` so demo / demo2 do not share state.
 * TradingView's own localStorage featuresets stay disabled; this module is
 * the only client persistence besides sessionStorage JWT.
 *
 * Factory snapshot (`ctfx:factory-chart`) is shared: USD/JPY 1D with volume.
 */

import type { ThemeName } from 'charting_library';
import { getToken } from './auth.ts';
import type { PriceMode } from './fx/types.ts';

export const DEFAULT_SYMBOL = 'USD/JPY';
export const DEFAULT_INTERVAL = '1D';

export interface ChartPrefs {
	priceMode: PriceMode;
	theme: ThemeName;
	lastSymbol: string;
	lastInterval: string;
	lastLayoutId?: string;
}

const FACTORY_KEY = 'ctfx:factory-chart';

function prefsKey(customerNo: string): string {
	return `ctfx:${customerNo}:prefs`;
}

function draftKey(customerNo: string): string {
	return `ctfx:${customerNo}:draft`;
}

function readJson(key: string): unknown {
	try {
		const raw = localStorage.getItem(key);
		if (!raw) {
			return null;
		}
		return JSON.parse(raw) as unknown;
	} catch {
		return null;
	}
}

function writeJson(key: string, value: unknown): void {
	try {
		localStorage.setItem(key, JSON.stringify(value));
	} catch {
		// Quota or private mode — prefs are optional.
	}
}

/**
 * Tenant id from the access JWT `customer_no` claim.
 *
 * @returns numeric customer id as string, or `anon` if missing
 */
export function getCustomerNo(): string {
	const token = getToken();
	if (!token) {
		return 'anon';
	}
	const payload = decodeJwtPayload(token);
	const customerNo = payload?.customer_no;
	if (typeof customerNo === 'number' && Number.isFinite(customerNo)) {
		return String(customerNo);
	}
	if (typeof customerNo === 'string' && customerNo.length > 0) {
		return customerNo;
	}
	return 'anon';
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
	const parts = token.split('.');
	if (parts.length < 2) {
		return null;
	}
	try {
		const normalized = parts[1].replace(/-/g, '+').replace(/_/g, '/');
		const padLength = (4 - (normalized.length % 4)) % 4;
		const padded = normalized + '='.repeat(padLength);
		return JSON.parse(atob(padded)) as Record<string, unknown>;
	} catch {
		return null;
	}
}

function isPriceMode(value: unknown): value is PriceMode {
	return value === 'bid' || value === 'ask' || value === 'mid';
}

function isThemeName(value: unknown): value is ThemeName {
	return value === 'dark' || value === 'light';
}

/**
 * Reads stored prefs for the logged-in customer.
 *
 * @returns last price mode and theme, or BID + caller should apply system theme
 */
export function loadChartPrefs(): Partial<ChartPrefs> {
	const stored = readJson(prefsKey(getCustomerNo()));
	if (!stored || typeof stored !== 'object') {
		return {};
	}
	const row = stored as Record<string, unknown>;
	const prefs: Partial<ChartPrefs> = {};
	if (isPriceMode(row.priceMode)) {
		prefs.priceMode = row.priceMode;
	}
	if (isThemeName(row.theme)) {
		prefs.theme = row.theme;
	}
	if (typeof row.lastSymbol === 'string' && row.lastSymbol.trim().length > 0) {
		prefs.lastSymbol = row.lastSymbol.trim();
	}
	if (typeof row.lastInterval === 'string' && row.lastInterval.trim().length > 0) {
		prefs.lastInterval = row.lastInterval.trim();
	}
	if (typeof row.lastLayoutId === 'string' && row.lastLayoutId.trim().length > 0) {
		prefs.lastLayoutId = row.lastLayoutId.trim();
	}
	return prefs;
}

function saveChartPrefs(patch: Partial<ChartPrefs>): void {
	const current = loadChartPrefs();
	const next: Partial<ChartPrefs> = { ...current, ...patch };
	if (patch.lastLayoutId === undefined && 'lastLayoutId' in patch) {
		delete next.lastLayoutId;
	}
	writeJson(prefsKey(getCustomerNo()), next);
}

/**
 * Persists BID / ASK / MID so the next page load uses the same history side.
 *
 * @param priceMode selected quote side
 */
export function savePriceMode(priceMode: PriceMode): void {
	saveChartPrefs({ priceMode });
}

/**
 * Persists light / dark header + pane theme.
 *
 * @param theme TradingView theme name
 */
export function saveTheme(theme: ThemeName): void {
	saveChartPrefs({ theme });
}

/**
 * Persists the last viewed pair and interval. First visit still opens
 * USD/JPY 1D; later refreshes re-apply these after a named layout loads.
 *
 * @param lastSymbol widget symbol, e.g. `EUR/USD`
 * @param lastInterval widget resolution, e.g. `1D`
 */
export function saveLastChartView(lastSymbol: string, lastInterval: string): void {
	const symbol = lastSymbol.trim();
	const interval = lastInterval.trim();
	if (!symbol || !interval) {
		return;
	}
	saveChartPrefs({ lastSymbol: symbol, lastInterval: interval });
}

/**
 * Remembers the named layout so the next refresh loads it instead of a draft.
 *
 * @param lastLayoutId server layout id
 */
export function saveLastLayoutId(lastLayoutId: string): void {
	const id = lastLayoutId.trim();
	if (!id) {
		return;
	}
	saveChartPrefs({ lastLayoutId: id });
}

/**
 * Clears the last layout id after that row is deleted.
 */
export function clearLastLayoutId(): void {
	saveChartPrefs({ lastLayoutId: undefined });
}

/**
 * Last unsaved widget.save() snapshot, used only when the server has no layouts.
 *
 * @returns chart state object, or null
 */
export function loadChartDraft(): object | null {
	const stored = readJson(draftKey(getCustomerNo()));
	if (!stored || typeof stored !== 'object' || Array.isArray(stored)) {
		return null;
	}
	return stored as object;
}

/**
 * Writes the current unsaved chart so a refresh can restore it when no
 * named layout exists.
 *
 * @param state widget.save() object
 */
export function saveChartDraft(state: object): void {
	writeJson(draftKey(getCustomerNo()), state);
}

/**
 * Factory USD/JPY 1D snapshot captured from a clean constructor chart.
 *
 * @returns chart state object, or null
 */
export function loadFactoryChart(): object | null {
	const stored = readJson(FACTORY_KEY);
	if (!stored || typeof stored !== 'object' || Array.isArray(stored)) {
		return null;
	}
	return stored as object;
}

/**
 * Stores the factory snapshot once so “apply default” after deleting all
 * layouts does not depend on an in-memory capture.
 *
 * @param state widget.save() object from a clean chart
 */
export function saveFactoryChart(state: object): void {
	if (loadFactoryChart()) {
		return;
	}
	writeJson(FACTORY_KEY, state);
}

/**
 * JSON string of the factory snapshot for SaveLoadAdapter getChartContent 404.
 *
 * @returns serialized state, or null
 */
export function getFactoryChartContent(): string | null {
	const factory = loadFactoryChart();
	return factory ? JSON.stringify(factory) : null;
}
