/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.dto.response.FxQuoteMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Java ingest publishes quotes to Redis and upserts the current open bar.
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
 *   <tr><td>1.1.0</td><td>2026/08/26</td><td>Task</td><td>Assert peach:forming snapshot</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/30</td><td>Task</td><td>Daily forming close tracks quote</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
 */
@SpringBootTest
class TickIngestWorkerTest {

	@Autowired
	private TickIngestWorker tickIngestWorker;

	@Autowired
	private DemoTickEngine demoTickEngine;

	@Autowired
	private StringRedisTemplate redis;

	@Autowired
	private ChartCacheStore chartCacheStore;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void tickPublishesQuoteSnapshotAndUpsertsOpenBar() throws Exception {
		tickIngestWorker.tick();

		String json = redis.opsForValue().get(QuoteBus.key("1"));
		assertThat(json).isNotBlank();
		JsonNode root = objectMapper.readTree(json);
		assertThat(root.get("curpairCd").asText()).isEqualTo("1");
		double bid = root.get("bid").asDouble();
		double ask = root.get("ask").asDouble();
		double mid = root.get("mid").asDouble();
		assertThat(bid).isLessThan(ask);
		assertThat(mid).isCloseTo((bid + ask) / 2.0, within(0.0000001));

		List<CachedChartBar> bars = chartCacheStore.query(
				CacheNamespace.CACHE_SET_1S,
				"USDJPY",
				null,
				null);
		assertThat(bars).isNotEmpty();
		CachedChartBar last = bars.get(bars.size() - 1);
		assertThat(last.bidClose()).isLessThan(last.askClose());

		String forming = redis.opsForValue().get(QuoteBus.formingKey("1S", "USDJPY"));
		assertThat(forming).isNotBlank();
		JsonNode formingRoot = objectMapper.readTree(forming);
		assertThat(formingRoot.get("curpairCd").asText()).isEqualTo("1");
		assertThat(formingRoot.get("resolution").asText()).isEqualTo("1S");
		assertThat(formingRoot.get("periodMs").asLong()).isEqualTo(1_000L);
		assertThat(formingRoot.get("time").asLong()).isGreaterThan(1_000_000_000_000L);
		assertThat(formingRoot.get("volume").asDouble()).isPositive();
		assertThat(formingRoot.get("bidClose").asDouble())
				.isLessThan(formingRoot.get("askClose").asDouble());
	}

	@Test
	void tickKeepsDailyFormingCloseInLockstepWithQuote() throws Exception {
		tickIngestWorker.tick();

		String quoteJson = redis.opsForValue().get(QuoteBus.key("1"));
		assertThat(quoteJson).isNotBlank();
		JsonNode quote = objectMapper.readTree(quoteJson);

		String formingJson = redis.opsForValue().get(QuoteBus.formingKey("1D", "USDJPY"));
		assertThat(formingJson).as("peach:forming:1D:USDJPY").isNotBlank();
		JsonNode forming = objectMapper.readTree(formingJson);
		assertThat(forming.get("bidClose").asDouble())
				.isCloseTo(quote.get("bid").asDouble(), within(0.1));
		assertThat(forming.get("askClose").asDouble())
				.isCloseTo(quote.get("ask").asDouble(), within(0.1));
	}

	@Test
	void bootSnapshotWritesFormingBarForDaily() throws Exception {
		String forming = redis.opsForValue().get(QuoteBus.formingKey("1D", "USDJPY"));
		assertThat(forming).as("peach:forming:1D:USDJPY").isNotBlank();
		JsonNode root = objectMapper.readTree(forming);
		assertThat(root.get("resolution").asText()).isEqualTo("1D");
		assertThat(root.get("periodMs").asLong()).isEqualTo(86_400_000L);
		assertThat(root.get("bidClose").asDouble()).isPositive();
	}

	@Test
	void snapshotAfterBootHasFivePairs() {
		for (int code = 1; code <= 5; code++) {
			String json = redis.opsForValue().get(QuoteBus.key(String.valueOf(code)));
			assertThat(json).as("peach:quote:%s", code).isNotBlank();
		}
	}

	@Test
	void demoTickEngineKeepsBidAskMidRelationship() {
		List<FxQuoteMessage> quotes = demoTickEngine.stepAll();
		assertThat(quotes).hasSize(5);
		for (FxQuoteMessage quote : quotes) {
			assertThat(quote.bid()).isLessThan(quote.ask());
			assertThat(quote.mid()).isCloseTo((quote.bid() + quote.ask()) / 2.0, within(0.0000001));
			assertThat(quote.high()).isGreaterThanOrEqualTo(quote.ask());
			assertThat(quote.low()).isLessThanOrEqualTo(quote.bid());
		}
	}
}
