package com.task.chart.config;

import com.task.chart.websocket.ChartStreamHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final ChartStreamHandler chartStreamHandler;
	private final AppProperties properties;

	public WebSocketConfig(ChartStreamHandler chartStreamHandler, AppProperties properties) {
		this.chartStreamHandler = chartStreamHandler;
		this.properties = properties;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		String[] origins = properties.getCorsOrigins().toArray(String[]::new);
		registry.addHandler(chartStreamHandler, "/ws/stream")
				.setAllowedOrigins(origins.length == 0 ? new String[] { "*" } : origins);
	}
}
