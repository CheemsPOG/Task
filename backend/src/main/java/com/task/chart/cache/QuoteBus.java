/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.dto.response.FxQuoteMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis snapshot + pub/sub bus for live FX quotes ({@code peach:quote:*} / {@code peach:quotes}).
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@Component
public class QuoteBus {

	/** Pub/sub channel Python WS subscribes to. */
	public static final String CHANNEL = "peach:quotes";

	/** Snapshot key prefix; suffix is numeric {@code curpairCd}. */
	public static final String KEY_PREFIX = "peach:quote:";

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
		redis.opsForValue().set(key(quote.curpairCd()), json);
		redis.convertAndSend(CHANNEL, json);
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

	private String toJson(FxQuoteMessage quote) {
		try {
			return objectMapper.writeValueAsString(quote);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize quote", ex);
		}
	}
}
