package com.task.chart.websocket;

import tools.jackson.databind.ObjectMapper;
import com.task.chart.dto.FxQuoteMessage;
import com.task.chart.service.MockFxQuoteService;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class FxQuoteWebSocketHandler extends TextWebSocketHandler implements MockFxQuoteService.QuoteListener {

	private static final Logger log = LoggerFactory.getLogger(FxQuoteWebSocketHandler.class);

	private final ObjectMapper objectMapper;
	private final MockFxQuoteService mockFxQuoteService;
	private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

	public FxQuoteWebSocketHandler(ObjectMapper objectMapper, MockFxQuoteService mockFxQuoteService) {
		this.objectMapper = objectMapper;
		this.mockFxQuoteService = mockFxQuoteService;
		this.mockFxQuoteService.addListener(this);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessions.add(session);
		sendQuietly(session, mockFxQuoteService.snapshot());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sessions.remove(session);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		log.warn("FX quote websocket error for {}: {}", session.getId(), exception.getMessage());
		sessions.remove(session);
	}

	@Override
	public void onQuotes(List<FxQuoteMessage> quotes) {
		for (WebSocketSession session : sessions) {
			sendQuietly(session, quotes);
		}
	}

	private void sendQuietly(WebSocketSession session, List<FxQuoteMessage> quotes) {
		if (!session.isOpen()) {
			sessions.remove(session);
			return;
		}
		try {
			synchronized (session) {
				for (FxQuoteMessage quote : quotes) {
					session.sendMessage(new TextMessage(objectMapper.writeValueAsString(quote)));
				}
			}
		} catch (IOException ex) {
			log.debug("Unable to push FX quote to {}: {}", session.getId(), ex.getMessage());
			sessions.remove(session);
		}
	}
}
