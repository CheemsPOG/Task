/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.dto.response.FxQuoteMessage;
import com.task.chart.service.CurrencyPairService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runtime single source of truth for live ticks and live OHLC bars.
 *
 * <p>This class is the <strong>only</strong> writer of forming candles. Each scheduled
 * tick: {@link DemoTickEngine} (mock LP) → upsert the current open bar into
 * {@code t_chart_*} and Redis {@code cache_set_*} → {@link QuoteBus}
 * {@code peach:quotes} (ticks) and {@code peach:bars} (forming candles).
 * Python only relays those Redis channels. {@code GET /api/history} reads
 * {@link ChartCacheStore}; it does not compute OHLC.
 *
 * <p>To attach a real Peach feed, replace {@link DemoTickEngine} only. Keep this
 * worker, the warehouse, Redis keys, and the Python gateway.
 *
 * <p><strong>NOT:</strong> not {@code @Transactional} (each JDBC upsert is its own
 * statement so a pair failure is not rolled back as a batch); not a request handler
 * (do not look for this in controllers); not Python (gateway only relays
 * {@code peach:quotes}/{@code peach:bars}); not {@link ChartCacheWriter} (boot seed
 * only, {@code @Order(100)} before this {@code @Order(200)}); not candle math in
 * {@link ChartCacheStore} (that class only reads what this worker last wrote).
 * One failed pair currently aborts the rest of that tick — there is no per-pair
 * try/catch. That is deliberate: a warehouse/Redis outage should stop ingest rather
 * than silently skip pairs.
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
 *   <tr><td>1.1.0</td><td>2026/08/26</td><td>Task</td><td>Publish forming bars for WS relay</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 *   <tr><td>1.3.0</td><td>2026/08/30</td><td>Task</td><td>Live DWM bars follow 24/7 demo ticks</td></tr>
 *   <tr><td>1.3.1</td><td>2026/08/31</td><td>Task</td><td>Review NOT list: no per-pair isolation</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.3.1
 */
@Component
@Order(200)
public class TickIngestWorker implements ApplicationRunner {

	private final DemoTickEngine demoTickEngine;
	private final QuoteBus quoteBus;
	private final ChartCacheWriter chartCacheWriter;
	private final ChartCacheStore chartCacheStore;
	private final ChartBarRepository chartBarRepository;
	private final CurrencyPairService currencyPairService;
	private final Map<String, CachedChartBar> formingBars = new ConcurrentHashMap<>();

	/**
	 * Creates the worker.
	 *
	 * @param demoTickEngine mock LP (replace this bean for a real feed)
	 * @param quoteBus Redis quote + forming-bar bus
	 * @param chartCacheWriter boot seeder that must finish before ticks
	 * @param chartCacheStore Redis {@code cache_set_*} hot cache
	 * @param chartBarRepository warehouse {@code t_chart_*}
	 * @param currencyPairService pair catalog ({@code m_ccypairs})
	 */
	public TickIngestWorker(
			DemoTickEngine demoTickEngine,
			QuoteBus quoteBus,
			ChartCacheWriter chartCacheWriter,
			ChartCacheStore chartCacheStore,
			ChartBarRepository chartBarRepository,
			CurrencyPairService currencyPairService) {
		this.demoTickEngine = demoTickEngine;
		this.quoteBus = quoteBus;
		this.chartCacheWriter = chartCacheWriter;
		this.chartCacheStore = chartCacheStore;
		this.chartBarRepository = chartBarRepository;
		this.currencyPairService = currencyPairService;
	}

	/**
	 * Aligns the mock LP with seeded 1S closes, then publishes quote and forming-bar
	 * snapshots so WebSocket clients that connect before the first scheduled tick
	 * still see prices.
	 *
	 * @param args unused Spring Boot arguments
	 */
	@Override
	public void run(ApplicationArguments args) {

		demoTickEngine.loadFromWarehouse();
		publishQuoteSnapshot();
		publishFormingSnapshot();
	}

	/**
	 * Steps the demo feed, upserts open bars, then publishes quotes.
	 *
	 * <p>This is the only scheduled OHLC writer. Python does not compute bars.
	 */
	@Scheduled(fixedRateString = "${app.chart-cache.tick-ms:333}")
	public void tick() {

		// ChartCacheWriter (@Order 100) must finish before ingest (@Order 200) ticks.
		if (!chartCacheWriter.isSeedComplete()) {
			return;
		}

		// Mock LP step. A real Peach feed replaces DemoTickEngine.stepAll() only.
		List<FxQuoteMessage> quotes = demoTickEngine.stepAll();

		for (FxQuoteMessage quote : quotes) {

			// Same pair: persist OHLC first, then header tick. No try/catch — one
			// JDBC/Redis failure stops remaining pairs this cycle (not silently skipped).
			upsertOpenBars(quote);
			quoteBus.publish(quote);
		}
	}

	/**
	 * Republishes the current tick snapshot without stepping the mock LP.
	 */
	private void publishQuoteSnapshot() {

		for (FxQuoteMessage quote : demoTickEngine.snapshot()) {
			quoteBus.publish(quote);
		}
	}

	/**
	 * Republishes the last stored bar per pair and resolution as the forming candle.
	 */
	private void publishFormingSnapshot() {

		for (CurrencyPairDto pair : currencyPairService.list()) {
			publishFormingSnapshotForPair(pair);
		}
	}

	/**
	 * Loads the last Redis bar for each namespace and seeds the in-memory forming map.
	 *
	 * @param pair catalog row
	 */
	private void publishFormingSnapshotForPair(CurrencyPairDto pair) {

		String numericCd = String.valueOf(pair.curpairCd());

		for (CacheNamespace namespace : CacheNamespace.values()) {
			List<CachedChartBar> bars = chartCacheStore.query(
					namespace,
					pair.curpairName(),
					null,
					null);

			if (bars.isEmpty()) {
				continue;
			}

			CachedChartBar last = bars.get(bars.size() - 1);
			formingBars.put(namespace.name() + ":" + pair.curpairName(), last);
			quoteBus.publishForming(numericCd, namespace, last);
		}
	}

	/**
	 * Upserts the current open bar for every Peach resolution of this tick's pair.
	 *
	 * @param quote live BID/ASK tick
	 */
	private void upsertOpenBars(FxQuoteMessage quote) {

		CurrencyPairDto pair = currencyPairService.find(Integer.parseInt(quote.curpairCd()));

		if (pair == null) {
			return;
		}

		long nowMs = Instant.now().toEpochMilli();

		for (CacheNamespace namespace : CacheNamespace.values()) {
			upsertOpenBar(namespace, pair.curpairName(), quote, nowMs);
		}
	}

	/**
	 * Writes one forming candle: new period opens a bar; same period applies the tick.
	 *
	 * @param namespace Peach table / cache mapping
	 * @param curpairName warehouse pair CD such as {@code USDJPY}
	 * @param quote live BID/ASK tick
	 * @param nowMs wall-clock millis used to bucket the open
	 */
	private void upsertOpenBar(
			CacheNamespace namespace,
			String curpairName,
			FxQuoteMessage quote,
			long nowMs) {

		long periodMs = namespace.periodMillis();
		long openMs = Math.floorDiv(nowMs - 1, periodMs) * periodMs;
		long openSec = openMs / 1000L;
		String formingKey = namespace.name() + ":" + curpairName;
		CachedChartBar previous = formingBars.get(formingKey);
		CachedChartBar updated;

		if (previous == null || previous.chartDatetimeSec() != openSec) {
			updated = CachedChartBar.openFromTick(curpairName, openSec, quote.bid(), quote.ask());
		} else {
			updated = previous.applyTick(quote.bid(), quote.ask());
		}

		// Persist then publish the same forming candle GET /api/history will return.
		formingBars.put(formingKey, updated);
		chartBarRepository.upsert(namespace, updated);
		chartCacheStore.put(namespace, updated);
		quoteBus.publishForming(quote.curpairCd(), namespace, updated);
	}
}
