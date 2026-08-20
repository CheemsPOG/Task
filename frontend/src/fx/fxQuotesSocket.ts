import { quoteStore } from './quoteStore.ts';

const INITIAL_RECONNECT_MS = 1_000;
const MAX_RECONNECT_MS = 30_000;

let socket: WebSocket | null = null;
let reconnectDelay = INITIAL_RECONNECT_MS;
let reconnectTimer: number | null = null;
let started = false;

function wsUrl(): string {
	const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
	return `${protocol}//${window.location.host}/ws/fx-quotes`;
}

function clearReconnect(): void {
	if (reconnectTimer !== null) {
		window.clearTimeout(reconnectTimer);
		reconnectTimer = null;
	}
}

function scheduleReconnect(): void {
	if (reconnectTimer !== null) {
		return;
	}
	reconnectTimer = window.setTimeout(() => {
		reconnectTimer = null;
		ensureSocket();
	}, reconnectDelay);
	reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_MS);
}

function ensureSocket(): void {
	if (
		socket &&
		(socket.readyState === WebSocket.OPEN ||
			socket.readyState === WebSocket.CONNECTING)
	) {
		return;
	}

	socket = new WebSocket(wsUrl());

	socket.addEventListener('open', () => {
		reconnectDelay = INITIAL_RECONNECT_MS;
		clearReconnect();
	});

	socket.addEventListener('message', event => {
		let payload: unknown;
		try {
			payload = JSON.parse(event.data);
		} catch {
			console.warn('[fx] ignored non-JSON quote message');
			return;
		}
		quoteStore.applyQuote(payload);
	});

	socket.addEventListener('close', () => {
		socket = null;
		if (!started) {
			return;
		}
		scheduleReconnect();
	});

	socket.addEventListener('error', () => {
		socket?.close();
	});
}

export function connectFxQuotes(): void {
	if (started) {
		return;
	}
	started = true;
	ensureSocket();
}
