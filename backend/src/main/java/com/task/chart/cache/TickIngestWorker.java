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
 * Runtime market ingest: mock ticks (Peach-feed stand-in) → warehouse/Redis bars + quote bus.
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
	 * @param demoTickEngine demo feed
	 * @param quoteBus Redis quote bus
	 * @param chartCacheWriter boot seeder
	 * @param chartCacheStore Redis bars
	 * @param chartBarRepository warehouse
	 * @param currencyPairService pair catalog
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

	@Override
	public void run(ApplicationArguments args) {
		demoTickEngine.loadFromWarehouse();
		for (FxQuoteMessage quote : demoTickEngine.snapshot()) {
			quoteBus.publish(quote);
		}
	}

	/**
	 * Steps the demo feed, publishes quotes, and upserts the current open bar on every namespace.
	 */
	@Scheduled(fixedRateString = "${app.chart-cache.tick-ms:333}")
	public void tick() {
		if (!chartCacheWriter.isSeedComplete()) {
			return;
		}
		List<FxQuoteMessage> quotes = demoTickEngine.stepAll();
		for (FxQuoteMessage quote : quotes) {
			quoteBus.publish(quote);
			upsertOpenBars(quote);
		}
	}

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

	private void upsertOpenBar(
			CacheNamespace namespace,
			String curpairName,
			FxQuoteMessage quote,
			long nowMs) {
		long periodMs = namespace.periodMillis();
		long openMs = Math.floorDiv(nowMs - 1, periodMs) * periodMs;
		if (namespace.skipWeekend(openMs)) {
			return;
		}
		long openSec = openMs / 1000L;
		String formingKey = namespace.name() + ":" + curpairName;
		CachedChartBar previous = formingBars.get(formingKey);
		CachedChartBar updated;
		if (previous == null || previous.chartDatetimeSec() != openSec) {
			updated = CachedChartBar.openFromTick(curpairName, openSec, quote.bid(), quote.ask());
		} else {
			updated = previous.applyTick(quote.bid(), quote.ask());
		}
		formingBars.put(formingKey, updated);
		chartBarRepository.upsert(namespace, updated);
		chartCacheStore.put(namespace, updated);
	}
}
