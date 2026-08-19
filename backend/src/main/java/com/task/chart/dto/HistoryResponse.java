package com.task.chart.dto;

import java.util.List;

public record HistoryResponse(
		String s,
		List<BarDto> bars,
		boolean noData,
		String errmsg) {

	public static HistoryResponse ok(List<BarDto> bars) {
		return new HistoryResponse("ok", bars, false, null);
	}

	public static HistoryResponse empty() {
		return new HistoryResponse("no_data", List.of(), true, null);
	}

	public static HistoryResponse error(String message) {
		return new HistoryResponse("error", List.of(), true, message);
	}
}
