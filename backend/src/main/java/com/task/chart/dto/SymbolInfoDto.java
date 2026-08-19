package com.task.chart.dto;

import java.util.List;

public record SymbolInfoDto(
		String ticker,
		String name,
		String description,
		String type,
		String exchange,
		String listed_exchange,
		String session,
		String timezone,
		int minmov,
		int pricescale,
		String format,
		boolean has_seconds,
		List<String> seconds_multipliers,
		boolean has_intraday,
		List<String> intraday_multipliers,
		boolean has_daily,
		List<String> daily_multipliers,
		boolean has_weekly_and_monthly,
		List<String> weekly_multipliers,
		List<String> monthly_multipliers,
		String visible_plots_set,
		List<String> supported_resolutions,
		String data_status,
		String provider_symbol) {
}
