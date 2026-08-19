package com.task.chart.dto;

public record BarDto(
		long time,
		double open,
		double high,
		double low,
		double close,
		double volume) {
}
