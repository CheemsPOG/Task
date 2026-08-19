package com.task.chart.service;

import java.util.List;
import java.util.Map;

/**
 * Maps TradingView resolutions to native Binance kline intervals.
 * The library aggregates extra UI resolutions (2, 4, 10, 90, 180, 5S, ...)
 * from the raw multipliers declared on each symbol.
 */
public final class ResolutionMapper {

	public static final String EXCHANGE = "Binance";

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

	private static final Map<String, String> BINANCE_INTERVALS = Map.ofEntries(
			Map.entry("1S", "1s"),
			Map.entry("1s", "1s"),
			Map.entry("1", "1m"),
			Map.entry("3", "3m"),
			Map.entry("5", "5m"),
			Map.entry("15", "15m"),
			Map.entry("30", "30m"),
			Map.entry("60", "1h"),
			Map.entry("120", "2h"),
			Map.entry("240", "4h"),
			Map.entry("360", "6h"),
			Map.entry("480", "8h"),
			Map.entry("720", "12h"),
			Map.entry("D", "1d"),
			Map.entry("1D", "1d"),
			Map.entry("3D", "3d"),
			Map.entry("W", "1w"),
			Map.entry("1W", "1w"),
			Map.entry("M", "1M"),
			Map.entry("1M", "1M"));

	private ResolutionMapper() {
	}

	public static String toBinanceInterval(String resolution) {
		if (resolution == null) {
			return null;
		}
		return BINANCE_INTERVALS.get(resolution);
	}
}
