/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Optional "Real-time / Delayed" badge on the symbol status API.
 *
 * Not installed from main.ts today. If wired, installSymbolStatus(widget)
 * then setSymbolStatus from resolveSymbol. Demo feed has delay 0 so it
 * always shows Real-time.
 */

import type {
	CustomStatusDropDownContent,
	IChartingLibraryWidget,
	ICustomSymbolStatusApi,
	LibrarySymbolInfo,
} from 'charting_library';

const REAL_TIME_COLOR = '#089981';
const DELAYED_COLOR = '#d97706';

const REAL_TIME_ICON = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
	<circle cx="10" cy="10" r="2" fill="currentColor" stroke="none" />
	<path d="M13.2 6.8a4.5 4.5 0 0 1 0 6.4" />
	<path d="M6.8 13.2a4.5 4.5 0 0 1 0-6.4" />
	<path d="M15.6 4.4a7.9 7.9 0 0 1 0 11.2" />
	<path d="M4.4 15.6a7.9 7.9 0 0 1 0-11.2" />
</svg>`;

const DELAYED_ICON = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
	<circle cx="10" cy="10" r="7" />
	<path d="M10 5.8v4.4l3.2 1.8" />
</svg>`;

const HTML_ESCAPES: Record<string, string> = Object.freeze({
	'&': '&amp;',
	'<': '&lt;',
	'>': '&gt;',
	'"': '&quot;',
	"'": '&#39;',
});

interface SymbolStatusDescriptor {
	symbolId: string;
	icon: string;
	color: string;
	tooltip: string;
	dropDownContent: CustomStatusDropDownContent[];
}

let statusApi: ICustomSymbolStatusApi | null = null;
const pendingStatuses = new Map<string, SymbolStatusDescriptor>();

function escapeHtml(value: unknown): string {
	return String(value ?? '').replace(
		/[&<>"']/g,
		character => HTML_ESCAPES[character] ?? character
	);
}

function describeFeed(symbolInfo: LibrarySymbolInfo) {
	const delaySeconds = symbolInfo.delay ?? 0;

	if (delaySeconds > 0) {
		const minutes = Math.max(1, Math.round(delaySeconds / 60));

		return {
			label: `Delayed ${minutes} min`,
			title: 'Delayed market data',
			summary: `This feed reports a ${minutes} minute delay, so the most recent bars lag the live market.`,
			icon: DELAYED_ICON,
			color: DELAYED_COLOR,
		};
	}

	return {
		label: 'Real-time',
		title: 'Real-time market data',
		summary:
			'Prices stream from the local demo feed, so bars and quotes update as simulated ticks happen.',
		icon: REAL_TIME_ICON,
		color: REAL_TIME_COLOR,
	};
}

function describeSymbolStatus(
	symbolInfo: LibrarySymbolInfo,
	providerSymbol: string
): SymbolStatusDescriptor {
	const feed = describeFeed(symbolInfo);

	return {
		symbolId: symbolInfo.ticker ?? symbolInfo.name,
		icon: feed.icon,
		color: feed.color,
		tooltip: `${feed.label} · ${symbolInfo.exchange}`,
		dropDownContent: [
			{
				title: feed.title,
				content: [
					`${feed.summary}<br/><br/>`,
					`Feed symbol: <code>${escapeHtml(providerSymbol)}</code><br/>`,
					`Exchange: ${escapeHtml(symbolInfo.exchange)}<br/>`,
					`Session: ${escapeHtml(symbolInfo.session)} · ${escapeHtml(symbolInfo.timezone)}`,
				],
				action: {
					text: 'Demo market data',
					tooltip: 'Local simulated quotes',
					onClick: () => undefined,
				},
			},
		],
	};
}

function applyStatus(status: SymbolStatusDescriptor): void {
	if (!statusApi) {
		return;
	}

	statusApi
		.symbol(status.symbolId)
		.setVisible(true)
		.setIcon(status.icon)
		.setColor(status.color)
		.setTooltip(status.tooltip)
		.setDropDownContent(status.dropDownContent);
}

export function setSymbolStatus(
	symbolInfo: LibrarySymbolInfo,
	providerSymbol: string
): void {
	const status = describeSymbolStatus(symbolInfo, providerSymbol);

	if (!statusApi) {
		pendingStatuses.set(status.symbolId, status);
		return;
	}

	applyStatus(status);
}

export function installSymbolStatus(widget: IChartingLibraryWidget): void {
	widget.headerReady().then(() => {
		statusApi = widget.customSymbolStatus();

		pendingStatuses.forEach(status => applyStatus(status));
		pendingStatuses.clear();
	});
}
