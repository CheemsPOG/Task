package com.task.chart.dto;

public record SearchSymbolDto(
		String symbol,
		String full_name,
		String ticker,
		String description,
		String exchange,
		String type) {
}
