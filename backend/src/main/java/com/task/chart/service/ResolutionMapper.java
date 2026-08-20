package com.task.chart.service;

import java.util.List;
import java.util.Map;

/**
 * Maps TradingView resolutions to native bar periods.
 * The library aggregates extra UI resolutions (2, 4, 10, 90, 180, 5S, ...)
 * from the raw multipliers declared on each symbol.
 */
public final class ResolutionMapper {

	public static final List<String> SUPPORTED_RESOLUTIONS = List.of(
			"1S", "5S", "15S", "30S",
			"1", "2", "3", "4", "5", "10", "15", "30",
			"60", "90", "120", "180", "240", "360", "480", "720",
			"1D", "3D", "1W", "1M");

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
}
