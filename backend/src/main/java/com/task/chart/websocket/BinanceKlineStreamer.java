package com.task.chart.websocket;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.task.chart.config.AppProperties;
import com.task.chart.dto.BarDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Shared Binance kline websocket. Multiple chart subscribers can share one stream.
 */
@Component
public class BinanceKlineStreamer {

	private static final Logger log = LoggerFactory.getLogger(BinanceKlineStreamer.class);
	private static final long MAX_RECONNECT_DELAY_MS = 30_000;

	private final AppProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "binance-kline-reconnect");
		thread.setDaemon(true);
		return thread;
	});

	private final Map<String, Set<Consumer<BarDto>>> listenersByStream = new ConcurrentHashMap<>();
	private final AtomicInteger requestId = new AtomicInteger();

	private final Object socketLock = new Object();
	private WebSocket webSocket;
	private long reconnectDelayMs = 1_000;
	private ScheduledFuture<?> reconnectTask;
	private boolean hasConnectedBefore;

	public BinanceKlineStreamer(AppProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public void subscribe(String streamName, Consumer<BarDto> listener) {
		listenersByStream.computeIfAbsent(streamName, key -> new CopyOnWriteArraySet<>()).add(listener);
		ensureSocket();
		sendSubscribe(streamName);
	}

	public void unsubscribe(String streamName, Consumer<BarDto> listener) {
		Set<Consumer<BarDto>> listeners = listenersByStream.get(streamName);
		if (listeners == null) {
			return;
		}
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			listenersByStream.remove(streamName);
			sendUnsubscribe(streamName);
		}
		if (listenersByStream.isEmpty()) {
			closeSocket();
		}
	}

	private void ensureSocket() {
		synchronized (socketLock) {
			if (webSocket != null) {
				return;
			}
			connect();
		}
	}

	private void connect() {
		cancelReconnect();
		httpClient.newWebSocketBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.buildAsync(URI.create(properties.getBinance().getWsUrl()), new BinanceListener())
				.whenComplete((socket, error) -> {
					if (error != null) {
						log.warn("Binance websocket connect failed: {}", error.getMessage());
						scheduleReconnect();
						return;
					}
					synchronized (socketLock) {
						webSocket = socket;
						reconnectDelayMs = 1_000;
					}
					if (hasConnectedBefore) {
						listenersByStream.values().forEach(listeners ->
								listeners.forEach(listener -> listener.accept(null)));
					}
					hasConnectedBefore = true;
					listenersByStream.keySet().forEach(this::sendSubscribe);
				});
	}

	private void sendSubscribe(String streamName) {
		sendControl("SUBSCRIBE", streamName);
	}

	private void sendUnsubscribe(String streamName) {
		sendControl("UNSUBSCRIBE", streamName);
	}

	private void sendControl(String method, String streamName) {
		WebSocket socket;
		synchronized (socketLock) {
			socket = webSocket;
		}
		if (socket == null) {
			return;
		}
		try {
			String payload = objectMapper.writeValueAsString(Map.of(
					"method", method,
					"params", List.of(streamName),
					"id", requestId.incrementAndGet()));
			socket.sendText(payload, true);
		} catch (Exception ex) {
			log.warn("Unable to send {} for {}: {}", method, streamName, ex.getMessage());
		}
	}

	private void handleMessage(String raw) {
		try {
			JsonNode message = objectMapper.readTree(raw);
			if (message.has("id") || message.has("result")) {
				return;
			}
			if (!"kline".equals(message.path("e").asText())) {
				return;
			}
			JsonNode kline = message.path("k");
			String streamName = kline.path("s").asText("").toLowerCase() + "@kline_" + kline.path("i").asText();
			BarDto bar = new BarDto(
					kline.path("t").asLong(),
					kline.path("o").asDouble(),
					kline.path("h").asDouble(),
					kline.path("l").asDouble(),
					kline.path("c").asDouble(),
					kline.path("v").asDouble());
			Set<Consumer<BarDto>> listeners = listenersByStream.get(streamName);
			if (listeners != null) {
				listeners.forEach(listener -> listener.accept(bar));
			}
		} catch (Exception ex) {
			log.debug("Ignoring Binance message: {}", ex.getMessage());
		}
	}

	private void scheduleReconnect() {
		if (listenersByStream.isEmpty()) {
			return;
		}
		synchronized (socketLock) {
			webSocket = null;
			if (reconnectTask != null && !reconnectTask.isDone()) {
				return;
			}
			long delay = reconnectDelayMs;
			reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
			reconnectTask = scheduler.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
		}
	}

	private void closeSocket() {
		synchronized (socketLock) {
			cancelReconnect();
			if (webSocket != null) {
				try {
					webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "idle");
				} catch (Exception ignored) {
					// ignore
				}
				webSocket = null;
			}
			reconnectDelayMs = 1_000;
		}
	}

	private void cancelReconnect() {
		if (reconnectTask != null) {
			reconnectTask.cancel(false);
			reconnectTask = null;
		}
	}

	private final class BinanceListener implements WebSocket.Listener {
		private final StringBuilder textBuffer = new StringBuilder();

		@Override
		public void onOpen(WebSocket webSocket) {
			webSocket.request(1);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			textBuffer.append(data);
			if (last) {
				String payload = textBuffer.toString();
				textBuffer.setLength(0);
				handleMessage(payload);
			}
			webSocket.request(1);
			return null;
		}

		@Override
		public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
			webSocket.request(1);
			return null;
		}

		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			scheduleReconnect();
			return null;
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			log.warn("Binance websocket error: {}", error.getMessage());
			scheduleReconnect();
		}
	}
}
