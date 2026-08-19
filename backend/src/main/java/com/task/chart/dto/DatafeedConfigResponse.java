package com.task.chart.dto;

import java.util.List;

public record DatafeedConfigResponse(
		boolean supports_search,
		boolean supports_group_request,
		boolean supports_marks,
		boolean supports_timescale_marks,
		boolean supports_time,
		List<String> supported_resolutions,
		List<ExchangeDto> exchanges,
		List<SymbolTypeDto> symbols_types) {

	public record ExchangeDto(String value, String name, String desc) {
	}

	public record SymbolTypeDto(String name, String value) {
	}
}
