/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.dto.response.FormingBarMessage;
import com.task.chart.dto.response.FxQuoteMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis snapshot + pub/sub for live FX quotes and forming bars.
 *
 * <p>{@code peach:quote:*} / {@code peach:quotes} = ticks (header).
 * {@code peach:forming:*} / {@code peach:bars} = forming candles (same OHLC
 * {@code GET /api/history} last bar already stored). Python WS only
 * {@code SUBSCRIBE}s these channels; it does not generate prices or OHLC.
 *
 * <p>Published by {@link TickIngestWorker}, the only live OHLC writer.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/26</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.1.0</td><td>2026/08/26</td><td>Task</td><td>Forming-bar bus for WS relay</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
 */
@Component
public class QuoteBus {

	/** Pub/sub channel Python WS subscribes to for ticks. */
	public static final String CHANNEL = "peach:quotes";

	/** Snapshot key prefix; suffix is numeric {@code curpairCd}. */
	public static final String KEY_PREFIX = "peach:quote:";

	/** Pub/sub channel for forming bars ({@code /ws/stream}). */
	public static final String BAR_CHANNEL = "peach:bars";

	/** Snapshot key prefix; suffix is {@code {tvResolution}:{curpairName}}. */
	public static final String FORMING_KEY_PREFIX = "peach:forming:";

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Creates the bus.
	 *
	 * @param redis string Redis template
	 */
	public QuoteBus(StringRedisTemplate redis) {
		this.redis = redis;
	}

	/**
	 * Stores the latest quote and publishes it for WebSocket gateways.
	 *
	 * @param quote tick payload
	 */
	public void publish(FxQuoteMessage quote) {

		String json = toJson(quote);

		// Snapshot for reconnecting clients, then fan-out the same JSON on peach:quotes.
		redis.opsForValue().set(key(quote.curpairCd()), json);
		redis.convertAndSend(CHANNEL, json);
	}

	/**
	 * Stores the latest forming bar for one resolution and publishes it for {@code /ws/stream}.
	 *
	 * @param numericCd catalog pair code as string
	 * @param namespace bar period
	 * @param bar warehouse row already upserted by {@link TickIngestWorker}
	 */
	public void publishForming(String numericCd, CacheNamespace namespace, CachedChartBar bar) {

		FormingBarMessage message = new FormingBarMessage(
				numericCd,
				bar.curpairCd(),
				namespace.tvResolution(),
				namespace.periodMillis(),
				bar.chartDatetimeSec() * 1000L,
				bar.bidOpen(),
				bar.bidHigh(),
				bar.bidLow(),
				bar.bidClose(),
				bar.askOpen(),
				bar.askHigh(),
				bar.askLow(),
				bar.askClose(),
				bar.volume());
		String json = toJson(message);

		// Snapshot peach:forming:{resolution}:{CD}, then fan-out on peach:bars.
		redis.opsForValue().set(formingKey(message.resolution(), message.curpairName()), json);
		redis.convertAndSend(BAR_CHANNEL, json);
	}

	/**
	 * Redis key for one pair snapshot.
	 *
	 * @param curpairCd numeric pair code as string
	 * @return key
	 */
	public static String key(String curpairCd) {
		return KEY_PREFIX + curpairCd;
	}

	/**
	 * Redis key for the current forming bar of one pair and TV resolution.
	 *
	 * @param resolution TradingView resolution such as {@code 1D}
	 * @param curpairName pair CD such as {@code USDJPY}
	 * @return key
	 */
	public static String formingKey(String resolution, String curpairName) {
		return FORMING_KEY_PREFIX + resolution + ":" + curpairName;
	}

	/**
	 * Serializes a tick or forming-bar payload for Redis.
	 *
	 * @param value {@link FxQuoteMessage} or {@link FormingBarMessage}
	 * @return JSON string
	 */
	private String toJson(Object value) {

		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize market payload", ex);
		}
	}
}
