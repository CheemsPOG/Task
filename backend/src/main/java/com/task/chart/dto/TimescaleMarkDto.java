package com.task.chart.dto;

import java.util.List;

public record TimescaleMarkDto(
		String id,
		long time,
		String color,
		String label,
		String labelFontColor,
		List<String> tooltip) {
}
