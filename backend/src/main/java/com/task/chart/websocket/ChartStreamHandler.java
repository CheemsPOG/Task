package com.task.chart.websocket;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.task.chart.dto.BarDto;
import com.task.chart.service.ChartDataService;
import com.task.chart.service.ResolutionMapper;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChartStreamHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(ChartStreamHandler.class);

	private final ObjectMapper objectMapper;
	private final ChartDataService chartDataService;
	private final MockKlineStreamer streamer;
	private final Map<String, Map<String, SessionSubscription>> sessions = new ConcurrentHashMap<>();

	public ChartStreamHandler(
			ObjectMapper objectMapper,
			ChartDataService chartDataService,
			MockKlineStreamer streamer) {
		this.objectMapper = objectMapper;
		this.chartDataService = chartDataService;
		this.streamer = streamer;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessions.put(session.getId(), new ConcurrentHashMap<>());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		JsonNode root = objectMapper.readTree(message.getPayload());
		String action = root.path("action").asText();
		String uid = root.path("uid").asText();

		if ("subscribe".equals(action)) {
			subscribe(session, uid, root.path("symbol").asText(), root.path("resolution").asText());
			return;
		}
		if ("unsubscribe".equals(action)) {
			unsubscribe(session, uid);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		clearSession(session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		log.warn("Chart websocket error for {}: {}", session.getId(), exception.getMessage());
		clearSession(session.getId());
	}

	private void subscribe(WebSocketSession session, String uid, String symbolName, String resolution) throws IOException {
		unsubscribe(session, uid);

		CachedSymbol symbol = chartDataService.findSymbol(symbolName);
		Long periodMs = ResolutionMapper.periodMillis(resolution);
		if (symbol == null || periodMs == null) {
			send(session, Map.of(
					"type", "error",
					"uid", uid,
					"message", "Cannot subscribe to " + symbolName + " @ " + resolution));
			return;
		}

		SessionSubscription subscription = new SessionSubscription(session, uid, symbol, periodMs, bar -> {
			if (bar == null) {
				sendQuietly(session, Map.of("type", "reset", "uid", uid));
				return;
			}
			sendQuietly(session, Map.of("type", "bar", "uid", uid, "bar", bar));
		});
		sessions.computeIfAbsent(session.getId(), key -> new ConcurrentHashMap<>()).put(uid, subscription);
		streamer.subscribe(symbol, periodMs, subscription.listener());
	}

	private void unsubscribe(WebSocketSession session, String uid) {
		Map<String, SessionSubscription> byUid = sessions.get(session.getId());
		if (byUid == null) {
			return;
		}
		SessionSubscription subscription = byUid.remove(uid);
		if (subscription != null) {
			streamer.unsubscribe(subscription.symbol(), subscription.periodMs(), subscription.listener());
		}
	}

	private void clearSession(String sessionId) {
		Map<String, SessionSubscription> byUid = sessions.remove(sessionId);
		if (byUid == null) {
			return;
		}
		byUid.values().forEach(subscription ->
				streamer.unsubscribe(subscription.symbol(), subscription.periodMs(), subscription.listener()));
	}

	private void send(WebSocketSession session, Object payload) throws IOException {
		if (!session.isOpen()) {
			return;
		}
		synchronized (session) {
			session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
		}
	}

	private void sendQuietly(WebSocketSession session, Object payload) {
		try {
			send(session, payload);
		} catch (IOException ex) {
			log.debug("Unable to push bar to {}: {}", session.getId(), ex.getMessage());
		}
	}

	private record SessionSubscription(
			WebSocketSession session,
			String uid,
			CachedSymbol symbol,
			long periodMs,
			java.util.function.Consumer<BarDto> listener) {
	}
}
