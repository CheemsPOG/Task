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

function ensureSocket(): WebSocket {
	if (
		socket &&
		(socket.readyState === WebSocket.OPEN ||
			socket.readyState === WebSocket.CONNECTING)
	) {
		return socket;
	}

	socket = new WebSocket(wsUrl());

	socket.addEventListener('open', () => {
		const ws = socket;
		reconnectDelay = 1_000;

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
			ws.send(
				JSON.stringify({
					action: 'subscribe',
					uid,
					symbol: handler.symbol,
					resolution: handler.resolution,
					price: handler.price,
				})
			);
		});
	});

	socket.addEventListener('message', event => {
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

	socket.addEventListener('close', () => {
		socket = null;
		if (subscriberToHandler.size === 0) return;

		reconnectTimer = window.setTimeout(() => {
			reconnectDelay = Math.min(reconnectDelay * 2, 30_000);
			ensureSocket();
		}, reconnectDelay);
	});

	socket.addEventListener('error', () => {
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

	const ws = ensureSocket();
	if (ws.readyState === WebSocket.OPEN) {
		ws.send(
			JSON.stringify({
				action: 'subscribe',
				uid: subscriberUID,
				symbol: symbolInfo.ticker ?? symbolInfo.name,
				resolution,
				price: quoteStore.mode,
			})
		);
	}
}

/**
 * BID/ASK/MID changed: drop pending bars and subscribe again on the same uid
 * so the widget does not keep the old side's candle.
 */
export function resubscribeAllWithCurrentPrice(): void {
	subscriberToHandler.forEach((handler, uid) => {
		handler.price = quoteStore.mode;
		pendingByUid.delete(uid);
		if (socket?.readyState !== WebSocket.OPEN) {
			return;
		}
		socket.send(JSON.stringify({ action: 'unsubscribe', uid }));
		socket.send(
			JSON.stringify({
				action: 'subscribe',
				uid,
				symbol: handler.symbol,
				resolution: handler.resolution,
				price: quoteStore.mode,
			})
		);
	});
}

export function unsubscribeFromStream(subscriberUID: string): void {
	pendingByUid.delete(subscriberUID);
	subscriberToHandler.delete(subscriberUID);

	if (socket?.readyState === WebSocket.OPEN) {
		socket.send(JSON.stringify({ action: 'unsubscribe', uid: subscriberUID }));
	}

	if (subscriberToHandler.size === 0) {
		if (reconnectTimer) {
			window.clearTimeout(reconnectTimer);
			reconnectTimer = null;
		}
		socket?.close();
		socket = null;
		reconnectDelay = 1_000;
	}
}
