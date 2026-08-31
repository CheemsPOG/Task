/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Realtime candles for the widget: WebSocket /ws/stream (Python gateway).
 *
 * Java TickIngestWorker is the only OHLC writer. Python relays forming bars
 * already stored in Redis (peach:forming:* / peach:bars). This file must not
 * invent open/high/low from ticks — that caused the chart Y-axis plunge.
 *
 * Vite proxies /ws to Python :8081. Messages:
 *   client → { action: subscribe|unsubscribe, uid, symbol, resolution, price }
 *   server → { type: bar, uid, bar: { time, open, high, low, close, volume } }
 *
 * Incoming bars are buffered and flushed every 250ms so the library is not
 * flooded. Bars older than the last history bar are dropped (TV time order).
 */

import type {
	Bar,
	LibrarySymbolInfo,
	ResolutionString,
	SubscribeBarsCallback,
} from 'charting_library';
import { quoteStore } from '../fx/quoteStore.ts';

const UPDATE_FREQUENCY = 250;

interface SubscriberHandler {
	symbol: string;
	resolution: ResolutionString;
	price: string;
	callback: SubscribeBarsCallback;
	onResetCacheNeededCallback?: () => void;
	lastBarTime: number;
}

interface StreamMessage {
	uid?: string;
	type?: string;
	bar?: Bar;
}

let socket: WebSocket | null = null;
let reconnectDelay = 1_000;
let reconnectTimer: number | null = null;
let hasConnectedBefore = false;
const subscriberToHandler = new Map<string, SubscriberHandler>();
const pendingByUid = new Map<string, Bar>();

function wsUrl(): string {
	const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
	return `${protocol}//${window.location.host}/ws/stream`;
}

function isCurrentSocket(event: Event): boolean {
	return socket !== null && event.target === socket;
}

function clearReconnect(): void {
	if (reconnectTimer === null) {
		return;
	}
	window.clearTimeout(reconnectTimer);
	reconnectTimer = null;
}

function sendSubscribe(ws: WebSocket, uid: string, handler: SubscriberHandler): void {
	ws.send(
		JSON.stringify({
			action: 'subscribe',
			uid,
			symbol: handler.symbol,
			resolution: handler.resolution,
			price: handler.price,
		})
	);
}

function ensureSocket(): WebSocket {
	if (
		socket &&
		(socket.readyState === WebSocket.OPEN ||
			socket.readyState === WebSocket.CONNECTING)
	) {
		return socket;
	}

	socket = new WebSocket(wsUrl());

	socket.addEventListener('open', event => {
		if (!isCurrentSocket(event)) {
			return;
		}
		const ws = socket;
		reconnectDelay = 1_000;
		clearReconnect();

		if (hasConnectedBefore) {
			subscriberToHandler.forEach(handler => {
				handler.onResetCacheNeededCallback?.();
			});
		}
		hasConnectedBefore = true;

		if (!ws || ws.readyState !== WebSocket.OPEN) {
			return;
		}

		subscriberToHandler.forEach((handler, uid) => {
			sendSubscribe(ws, uid, handler);
		});
	});

	socket.addEventListener('message', event => {
		if (!isCurrentSocket(event)) {
			return;
		}
		let message: StreamMessage;
		try {
			message = JSON.parse(event.data) as StreamMessage;
		} catch {
			return;
		}

		const handler = subscriberToHandler.get(message.uid ?? '');
		if (!handler) return;

		if (message.type === 'reset') {
			handler.onResetCacheNeededCallback?.();
			return;
		}

		if (message.type === 'bar' && message.bar) {
			pendingByUid.set(message.uid ?? '', message.bar);
		}
	});

	socket.addEventListener('close', event => {
		if (!isCurrentSocket(event)) {
			return;
		}
		socket = null;
		if (subscriberToHandler.size === 0) {
			return;
		}

		reconnectTimer = window.setTimeout(() => {
			reconnectDelay = Math.min(reconnectDelay * 2, 30_000);
			ensureSocket();
		}, reconnectDelay);
	});

	socket.addEventListener('error', event => {
		if (!isCurrentSocket(event)) {
			return;
		}
		socket?.close();
	});

	return socket;
}

setInterval(() => {
	pendingByUid.forEach((bar, uid) => {
		const handler = subscriberToHandler.get(uid);
		if (!handler) {
			pendingByUid.delete(uid);
			return;
		}
		// Drop ticks older than the last history/live bar (avoids TV time-order violations).
		if (bar.time < handler.lastBarTime) {
			pendingByUid.delete(uid);
			return;
		}
		handler.lastBarTime = bar.time;
		handler.callback(bar);
		pendingByUid.delete(uid);
	});
}, UPDATE_FREQUENCY);

/**
 * Widget subscribeBars: keep last history bar time, then ask Python for
 * forming bars of this symbol / resolution / BID-ASK-MID.
 */
export function subscribeOnStream(
	symbolInfo: LibrarySymbolInfo,
	resolution: ResolutionString,
	onRealtimeCallback: SubscribeBarsCallback,
	subscriberUID: string,
	onResetCacheNeededCallback: () => void,
	lastBarTime = 0
): void {
	subscriberToHandler.set(subscriberUID, {
		symbol: symbolInfo.ticker ?? symbolInfo.name,
		resolution,
		price: quoteStore.mode,
		callback: onRealtimeCallback,
		onResetCacheNeededCallback,
		lastBarTime,
	});

	clearReconnect();
	const ws = ensureSocket();
	if (ws.readyState === WebSocket.OPEN) {
		const handler = subscriberToHandler.get(subscriberUID);
		if (handler) {
			sendSubscribe(ws, subscriberUID, handler);
		}
	}
}

/**
 * BID/ASK/MID changed: drop pending bars, invalidate TradingView's bar cache,
 * then subscribe again on the same uid so candles use the new side.
 *
 * onResetCacheNeededCallback must run before chart.resetData() (see
 * IChartWidgetApi.resetData). Without it the library keeps USD/JPY bars
 * cached and never calls getBars. Callbacks are collected first because
 * the library may unsubscribe during the callback and mutate this map.
 */
export function resubscribeAllWithCurrentPrice(): void {
	const resetCallbacks: Array<() => void> = [];
	subscriberToHandler.forEach((handler, uid) => {
		handler.price = quoteStore.mode;
		pendingByUid.delete(uid);
		if (handler.onResetCacheNeededCallback) {
			resetCallbacks.push(handler.onResetCacheNeededCallback);
		}
	});
	for (const resetCache of resetCallbacks) {
		resetCache();
	}

	subscriberToHandler.forEach((handler, uid) => {
		if (socket?.readyState !== WebSocket.OPEN) {
			return;
		}
		socket.send(JSON.stringify({ action: 'unsubscribe', uid }));
		sendSubscribe(socket, uid, handler);
	});
}

/**
 * Layout load unsubscribes every series then resubscribes after getBars.
 * Keep the socket open across that gap — closing it races the replacement
 * connection and the stale `close` handler nulls the new WebSocket.
 */
export function unsubscribeFromStream(subscriberUID: string): void {
	pendingByUid.delete(subscriberUID);
	subscriberToHandler.delete(subscriberUID);

	if (socket?.readyState === WebSocket.OPEN) {
		socket.send(JSON.stringify({ action: 'unsubscribe', uid: subscriberUID }));
	}

	if (subscriberToHandler.size === 0) {
		clearReconnect();
		reconnectDelay = 1_000;
	}
}

/** After a layout load: reconnect if the socket died, else re-send subscribes. */
export function ensureStreamAlive(): void {
	if (subscriberToHandler.size === 0) {
		return;
	}
	const ws = ensureSocket();
	if (ws.readyState !== WebSocket.OPEN) {
		return;
	}
	subscriberToHandler.forEach((handler, uid) => {
		sendSubscribe(ws, uid, handler);
	});
}
