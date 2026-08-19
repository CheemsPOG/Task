package com.task.chart.dto;

import java.util.List;

public record MarkDto(
		String id,
		long time,
		String color,
		String label,
		String labelFontColor,
		Integer minSize,
		List<String> text) {
}
