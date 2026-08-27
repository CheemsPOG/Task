/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

import java.util.List;

/**
 * Datafeed onReady configuration payload.
 *
 * <p>Design doc 120 {@code GET /api/config} tells the widget which UDF features this backend
 * supports. {@code ChartDataController.config} returns {@code ChartDataServiceImpl} output from
 * {@code app.tradingview} yml. JSON field names are snake_case because that is the TradingView
 * contract. It is not per-symbol metadata (design doc 123 / {@link SymbolInfoDto}).
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public record DatafeedConfigResponse(
		boolean supports_search,
		boolean supports_group_request,
		boolean supports_marks,
		boolean supports_timescale_marks,
		boolean supports_time,
		List<String> supported_resolutions,
		List<ExchangeDto> exchanges,
		List<SymbolTypeDto> symbols_types) {

	/** One exchange row in the UDF {@code exchanges} array. */
	public record ExchangeDto(String value, String name, String desc) {
	}

	/** One symbol-type row in the UDF {@code symbols_types} array. */
	public record SymbolTypeDto(String name, String value) {
	}
}
