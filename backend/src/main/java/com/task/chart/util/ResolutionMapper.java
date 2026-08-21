/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.util;

import java.util.List;
import java.util.Map;

/**
 * Maps TradingView resolutions to native bar periods.
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
public final class ResolutionMapper {

	public static final List<String> SUPPORTED_RESOLUTIONS = List.of(
			"1S", "5S", "15S", "30S",
			"1", "2", "3", "4", "5", "10", "15", "30",
			"60", "90", "120", "180", "240", "360", "480", "720",
			"1D", "3D", "1W", "1M");

	/** Resolutions accepted by design doc 121 validation. */
	public static final List<String> HISTORY_RESOLUTIONS = List.of(
			"1S", "1", "5", "10", "15", "30", "60", "120", "240", "480", "1D", "1W", "1M");

	public static final List<String> INTRADAY_MULTIPLIERS = List.of(
			"1", "3", "5", "15", "30", "60", "120", "240", "360", "480", "720");

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

	public static Long periodMillis(String resolution) {
		if (resolution == null) {
			return null;
		}
		return PERIOD_MS.get(resolution);
	}

	public static boolean isHistoryResolution(String resolution) {
		return resolution != null && HISTORY_RESOLUTIONS.contains(resolution);
	}
}
