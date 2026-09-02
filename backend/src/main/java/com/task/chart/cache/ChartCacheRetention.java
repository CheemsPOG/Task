/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

/**
 * Per-resolution bar cap for Redis {@code cache_set_*} ZSETs and {@code t_chart_*} warehouse rows.
 *
 * <p>Demo keeps a fixed newest-N window per namespace (not key TTL). {@link ChartCacheWriter}
 * seeds to this depth; {@link ChartCacheStore#put} and {@link ChartBarRepository#upsert} trim
 * older bars after each live write so ingest cannot grow Redis/Postgres unbounded.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/09/02</td><td>Task</td><td>Cap ZSET + warehouse by resolution</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
public final class ChartCacheRetention {

	private ChartCacheRetention() {
	}

	/**
	 * Maximum bars to retain per pair for one Peach namespace (newest by {@code chart_datetime}).
	 *
	 * @param namespace cache / warehouse mapping
	 * @return bar count cap
	 */
	public static int maxBars(CacheNamespace namespace) {

		return switch (namespace) {
			case CACHE_SET_1S -> 900;
			case CACHE_SET_1M -> 600;
			case CACHE_SET_5M, CACHE_SET_10M, CACHE_SET_15M, CACHE_SET_30M -> 400;
			case CACHE_SET_60M, CACHE_SET_120M, CACHE_SET_240M, CACHE_SET_480M -> 300;
			case CACHE_SET_DAY -> 400;
			case CACHE_SET_WEEK -> 200;
			case CACHE_SET_MONTH -> 120;
		};
	}
}
