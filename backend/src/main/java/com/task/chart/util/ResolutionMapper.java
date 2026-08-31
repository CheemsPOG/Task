/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.util;

import java.util.List;
import java.util.Map;

/**
 * Maps TradingView resolutions to native bar periods.
 *
 * <p>Converts widget strings ({@code 1}, {@code 1D}, {@code 1S}) to period milliseconds and Peach
 * {@code chart_type} ({@code 1M}, {@code DAY}). Doc 121 history accepts {@code 10}; docs 125/126 marks
 * do not. {@link com.task.chart.service.impl.ChartDataServiceImpl},
 * {@link com.task.chart.service.impl.ChartLayoutServiceImpl}, and {@code CacheNamespace} call this.
 * This is NOT the Python WS, NOT the widget {@code datafeed.ts} list, and TV {@code 1} is one minute
 * (Peach {@code 1M}), not one month.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/20</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public final class ResolutionMapper {

	/** Resolutions accepted by design doc 121 validation. */
	public static final List<String> HISTORY_RESOLUTIONS = List.of(
			"1S", "1", "5", "10", "15", "30", "60", "120", "240", "480", "1D", "1W", "1M");

	/** Resolutions accepted by design doc 125 marks (no {@code 10}). */
	public static final List<String> MARKS_RESOLUTIONS = List.of(
			"1S", "1", "5", "15", "30", "60", "120", "240", "480", "1D", "1W", "1M");

	public static final List<String> SECONDS_MULTIPLIERS = List.of("1");
	public static final List<String> DAILY_MULTIPLIERS = List.of("1", "3");
	public static final List<String> WEEKLY_MULTIPLIERS = List.of("1");
	public static final List<String> MONTHLY_MULTIPLIERS = List.of("1");

	private static final long SECOND = 1_000L;
	private static final long MINUTE = 60 * SECOND;
	private static final long HOUR = 60 * MINUTE;
	private static final long DAY = 24 * HOUR;

	private static final Map<String, Long> PERIOD_MS = Map.ofEntries(
			Map.entry("1S", SECOND),
			Map.entry("1s", SECOND),
			Map.entry("1", MINUTE),
			Map.entry("3", 3 * MINUTE),
			Map.entry("5", 5 * MINUTE),
			Map.entry("10", 10 * MINUTE),
			Map.entry("15", 15 * MINUTE),
			Map.entry("30", 30 * MINUTE),
			Map.entry("60", HOUR),
			Map.entry("120", 2 * HOUR),
			Map.entry("240", 4 * HOUR),
			Map.entry("360", 6 * HOUR),
			Map.entry("480", 8 * HOUR),
			Map.entry("720", 12 * HOUR),
			Map.entry("D", DAY),
			Map.entry("1D", DAY),
			Map.entry("3D", 3 * DAY),
			Map.entry("W", 7 * DAY),
			Map.entry("1W", 7 * DAY),
			Map.entry("M", 30 * DAY),
			Map.entry("1M", 30 * DAY));

	private ResolutionMapper() {
	}

	/**
	 * Period length in milliseconds for a TradingView resolution string.
	 *
	 * @param resolution widget resolution such as {@code 1} or {@code 1D}
	 * @return period ms, or {@code null} if unknown
	 */
	public static Long periodMillis(String resolution) {
		if (resolution == null) {
			return null;
		}

		return PERIOD_MS.get(resolution);
	}

	/**
	 * @param resolution widget resolution
	 * @return true when doc 121 accepts this resolution (includes {@code 10})
	 */
	public static boolean isHistoryResolution(String resolution) {
		return resolution != null && HISTORY_RESOLUTIONS.contains(resolution);
	}

	/**
	 * @param resolution widget resolution
	 * @return true when docs 125/126 accept this resolution (no {@code 10})
	 */
	public static boolean isMarksResolution(String resolution) {
		return resolution != null && MARKS_RESOLUTIONS.contains(resolution);
	}

	/**
	 * Maps TradingView widget resolution to Peach chart_type (design doc 121).
	 * Used for documentation / future cache routing; mock history uses {@link #periodMillis}.
	 *
	 * @param resolution TradingView resolution
	 * @return Peach chart_type, or {@code null} if unsupported for history
	 */
	public static String toPeachChartType(String resolution) {
		if (resolution == null) {
			return null;
		}

		// TV "1" is 1 minute → Peach "1M"; TV "1M" is 1 month → Peach "MONTH".
		return switch (resolution) {
			case "1S" -> "1S";
			case "1" -> "1M";
			case "5" -> "5M";
			case "10" -> "10M";
			case "15" -> "15M";
			case "30" -> "30M";
			case "60" -> "60M";
			case "120" -> "120M";
			case "240" -> "240M";
			case "480" -> "480M";
			case "1D" -> "DAY";
			case "1W" -> "WEEK";
			case "1M" -> "MONTH";
			default -> null;
		};
	}
}