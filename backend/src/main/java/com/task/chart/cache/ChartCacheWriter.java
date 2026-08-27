/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.task.chart.service.MockBarGenerator;
import com.task.chart.service.SymbolCatalog;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Design doc 121 boot seeder: historical {@code t_chart_*} and Redis {@code cache_set_*}.
 *
 * <p>Runs once at startup ({@code @Order(100)}) using {@link MockBarGenerator}.
 * After seed, live open bars come only from {@link TickIngestWorker}. This class
 * has no scheduled refresh.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/21</td><td>Task</td><td>Phase 1 in-memory / Redis</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/26</td><td>Task</td><td>Boot seed only; live bars from ingest</td></tr>
 *   <tr><td>1.3.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.3.0
 */
@Component
@Order(100)
public class ChartCacheWriter implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ChartCacheWriter.class);

	private final ChartBarRepository chartBarRepository;
	private final ChartCacheStore chartCacheStore;
	private final SymbolCatalog symbolCatalog;
	private final MockBarGenerator mockBarGenerator;
	private volatile boolean seedComplete;

	/**
	 * Creates the writer.
	 *
	 * @param chartBarRepository warehouse tables
	 * @param chartCacheStore Redis cache
	 * @param symbolCatalog pair catalog
	 * @param mockBarGenerator deterministic Peach bar factory
	 */
	public ChartCacheWriter(
			ChartBarRepository chartBarRepository,
			ChartCacheStore chartCacheStore,
			SymbolCatalog symbolCatalog,
			MockBarGenerator mockBarGenerator) {
		this.chartBarRepository = chartBarRepository;
		this.chartCacheStore = chartCacheStore;
		this.symbolCatalog = symbolCatalog;
		this.mockBarGenerator = mockBarGenerator;
	}

	/**
	 * Seeds warehouse and Redis, then allows {@link TickIngestWorker} to tick.
	 *
	 * @param args unused Spring Boot arguments
	 */
	@Override
	public void run(ApplicationArguments args) {

		seedAll();
		seedComplete = true;
	}

	/**
	 * Whether boot seed finished (ingest must not tick before this).
	 *
	 * @return true after {@link #seedAll()}
	 */
	public boolean isSeedComplete() {
		return seedComplete;
	}

	/**
	 * Full seed: warehouse tables then Redis namespaces.
	 */
	public synchronized void seedAll() {

		long toMs = Instant.now().toEpochMilli();
		List<CachedSymbol> symbols = symbolCatalog.getAll();
		int total = 0;

		// One pass per Peach namespace × catalog pair: warehouse then Redis.
		for (CacheNamespace namespace : CacheNamespace.values()) {
			int depth = seedDepth(namespace);

			for (CachedSymbol symbol : symbols) {
				List<CachedChartBar> bars = buildSeries(symbol, namespace, toMs, depth);
				chartBarRepository.replacePair(namespace, symbol.providerSymbol(), bars);
				chartCacheStore.replacePair(namespace, symbol.providerSymbol(), bars);
				total += bars.size();
			}
		}

		log.info(
				"Seeded Peach warehouse + Redis: {} bars across {} tables/namespaces × {} pairs",
				total,
				CacheNamespace.values().length,
				symbols.size());
	}

	/**
	 * Walks backward from the current open, skipping weekend day/week/month bars.
	 *
	 * @param symbol catalog pair
	 * @param namespace Peach table / cache mapping
	 * @param toMs seed-end wall clock
	 * @param depth number of bars to keep
	 * @return oldest-first series
	 */
	private List<CachedChartBar> buildSeries(
			CachedSymbol symbol,
			CacheNamespace namespace,
			long toMs,
			int depth) {

		long periodMs = namespace.periodMillis();
		long lastOpen = Math.floorDiv(toMs - 1, periodMs) * periodMs;
		java.util.LinkedHashMap<Long, CachedChartBar> bySec = new java.util.LinkedHashMap<>();
		long cursor = lastOpen;
		int guard = 0;
		int maxGuard = depth * 4 + 64;

		while (bySec.size() < depth && cursor >= 0 && guard < maxGuard) {
			guard++;

			if (!namespace.skipWeekend(cursor)) {
				CachedChartBar bar = mockBarGenerator.peachBarAt(symbol, periodMs, cursor);
				bySec.putIfAbsent(bar.chartDatetimeSec(), bar);
			}

			cursor -= periodMs;
		}

		return new ArrayList<>(bySec.values());
	}

	/**
	 * How many historical bars to seed per namespace (demo depths, not Peach production).
	 *
	 * @param namespace Peach table / cache mapping
	 * @return bar count
	 */
	private static int seedDepth(CacheNamespace namespace) {

		return switch (namespace) {
			case CACHE_SET_1S -> 900;
			case CACHE_SET_1M -> 600;
			case CACHE_SET_5M, CACHE_SET_10M, CACHE_SET_15M, CACHE_SET_30M -> 400;
			case CACHE_SET_60M, CACHE_SET_120M, CACHE_SET_240M, CACHE_SET_480M -> 300;
			case CACHE_SET_DAY -> 400;
			case CACHE_SET_WEEK -> 200;
			case CACHE_SET_MONTH -> 120;
			default -> 100;
		};
	}
}
