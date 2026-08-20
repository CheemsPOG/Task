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
		if (handler) {
			handler.callback(bar);
		}
		pendingByUid.delete(uid);
	});
}, UPDATE_FREQUENCY);

export function subscribeOnStream(
	symbolInfo: LibrarySymbolInfo,
	resolution: ResolutionString,
	onRealtimeCallback: SubscribeBarsCallback,
	subscriberUID: string,
	onResetCacheNeededCallback: () => void
): void {
	subscriberToHandler.set(subscriberUID, {
		symbol: symbolInfo.ticker ?? symbolInfo.name,
		resolution,
		price: quoteStore.mode,
		callback: onRealtimeCallback,
		onResetCacheNeededCallback,
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
