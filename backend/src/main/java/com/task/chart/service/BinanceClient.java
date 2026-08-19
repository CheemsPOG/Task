package com.task.chart.service;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

@Component
public class BinanceClient {

	private final RestClient restClient;

	public BinanceClient(@Qualifier("binanceRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public JsonNode exchangeInfo() {
		return restClient.get()
				.uri("/api/v3/exchangeInfo")
				.retrieve()
				.body(JsonNode.class);
	}

	public long serverTimeMillis() {
		JsonNode body = restClient.get()
				.uri("/api/v3/time")
				.retrieve()
				.body(JsonNode.class);
		if (body == null || !body.has("serverTime")) {
			return System.currentTimeMillis();
		}
		return body.get("serverTime").asLong();
	}

	public JsonNode klines(String symbol, String interval, Long endTime, int limit) {
		return restClient.get()
				.uri(uriBuilder -> buildKlinesUri(uriBuilder, symbol, interval, endTime, limit))
				.retrieve()
				.body(JsonNode.class);
	}

	private static java.net.URI buildKlinesUri(
			UriBuilder uriBuilder,
			String symbol,
			String interval,
			Long endTime,
			int limit) {
		uriBuilder.path("/api/v3/klines")
				.queryParam("symbol", symbol)
				.queryParam("interval", interval)
				.queryParam("limit", limit);
		if (endTime != null) {
			uriBuilder.queryParam("endTime", endTime);
		}
		return uriBuilder.build();
	}
}
