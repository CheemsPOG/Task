/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.task.chart.util.ResolutionMapper;

/**
 * Peach cache mapping from design doc 121 (chart_type → {@code t_chart_*} → {@code cache_set_*}).
 *
 * <p>Used by ingest, warehouse JDBC, and Redis keys. Never pass a request string
 * as a table name — resolve TradingView resolution through this enum only.
 *
 * <p><strong>Trap:</strong> TradingView {@code "1"} is 1 <em>minute</em>
 * ({@link #CACHE_SET_1M} → {@code t_chart_60} / {@code cache_set_1m}), not 1 month
 * ({@link #CACHE_SET_MONTH} / TV {@code "1M"}). Adding a resolution requires this
 * enum <em>and</em> {@link ResolutionMapper} <em>and</em> a Flyway {@code t_chart_*}
 * table.
 *
 * <p><strong>NOT:</strong> not a place to interpolate user input into SQL; not
 * weekend/session logic (demo ticks are 24/7); not the widget's own resolution list
 * in {@code datafeed.ts}.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/21</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/26</td><td>Task</td><td>skipWeekend helper</td></tr>
 *   <tr><td>1.3.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 *   <tr><td>1.4.0</td><td>2026/08/30</td><td>Task</td><td>skipWeekend is seeder-only</td></tr>
 *   <tr><td>1.5.0</td><td>2026/08/30</td><td>Task</td><td>Remove weekend skip (24/7 mock)</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.5.0
 */
public enum CacheNamespace {

	CACHE_SET_1S("1S", "t_chart_1", "cache_set_1s", "1S"),
	CACHE_SET_1M("1M", "t_chart_60", "cache_set_1m", "1"),
	CACHE_SET_5M("5M", "t_chart_300", "cache_set_5m", "5"),
	CACHE_SET_10M("10M", "t_chart_600", "cache_set_10m", "10"),
	CACHE_SET_15M("15M", "t_chart_900", "cache_set_15m", "15"),
	CACHE_SET_30M("30M", "t_chart_1800", "cache_set_30m", "30"),
	CACHE_SET_60M("60M", "t_chart_3600", "cache_set_60m", "60"),
	CACHE_SET_120M("120M", "t_chart_7200", "cache_set_120m", "120"),
	CACHE_SET_240M("240M", "t_chart_14400", "cache_set_240m", "240"),
	CACHE_SET_480M("480M", "t_chart_28800", "cache_set_480m", "480"),
	CACHE_SET_DAY("DAY", "t_chart_day", "cache_set_day", "1D"),
	CACHE_SET_WEEK("WEEK", "t_chart_week", "cache_set_week", "1W"),
	CACHE_SET_MONTH("MONTH", "t_chart_month", "cache_set_month", "1M");

	private final String chartType;
	private final String tableName;
	private final String cacheName;
	private final String tvResolution;

	CacheNamespace(String chartType, String tableName, String cacheName, String tvResolution) {
		this.chartType = chartType;
		this.tableName = tableName;
		this.cacheName = cacheName;
		this.tvResolution = tvResolution;
	}

	/**
	 * Peach chart_type for this namespace.
	 *
	 * @return chart type such as {@code DAY}
	 */
	public String chartType() {
		return chartType;
	}

	/**
	 * Warehouse table from doc 121 list ({@code t_chart_*}).
	 *
	 * @return table name
	 */
	public String tableName() {
		return tableName;
	}

	/**
	 * Redis {@code cache_set_*} name used in {@link ChartCacheStore} keys.
	 *
	 * @return cache name such as {@code cache_set_day}
	 */
	public String cacheName() {
		return cacheName;
	}

	/**
	 * TradingView resolution string the widget sends on {@code GET /api/history}.
	 *
	 * @return resolution such as {@code 1D}
	 */
	public String tvResolution() {
		return tvResolution;
	}

	/**
	 * Bar period in milliseconds.
	 *
	 * @return period
	 */
	public long periodMillis() {
		return ResolutionMapper.periodMillis(tvResolution);
	}

	/**
	 * Resolves namespace from TradingView resolution via Peach chart_type.
	 *
	 * @param resolution TradingView resolution
	 * @return namespace, or {@code null} if unsupported
	 */
	public static CacheNamespace fromTvResolution(String resolution) {

		String chartType = ResolutionMapper.toPeachChartType(resolution);

		if (chartType == null) {
			return null;
		}

		for (CacheNamespace namespace : values()) {

			if (namespace.chartType.equals(chartType)) {
				return namespace;
			}
		}

		return null;
	}
}
