package com.task.chart.config;

import com.task.chart.websocket.ChartStreamHandler;
import com.task.chart.websocket.FxQuoteWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final ChartStreamHandler chartStreamHandler;
	private final FxQuoteWebSocketHandler fxQuoteWebSocketHandler;
	private final AppProperties properties;

	public WebSocketConfig(
			ChartStreamHandler chartStreamHandler,
			FxQuoteWebSocketHandler fxQuoteWebSocketHandler,
			AppProperties properties) {
		this.chartStreamHandler = chartStreamHandler;
		this.fxQuoteWebSocketHandler = fxQuoteWebSocketHandler;
		this.properties = properties;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		String[] origins = properties.getCorsOrigins().toArray(String[]::new);
		registry.addHandler(chartStreamHandler, "/ws/stream")
				.setAllowedOrigins(origins.length == 0 ? new String[] { "*" } : origins);
		registry.addHandler(fxQuoteWebSocketHandler, "/ws/fx-quotes")
				.setAllowedOrigins(origins.length == 0 ? new String[] { "*" } : origins);
	}
}
