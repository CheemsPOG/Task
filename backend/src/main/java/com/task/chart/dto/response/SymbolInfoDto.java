/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

import java.util.List;

/**
 * Resolved symbol metadata for GET /api/symbols.
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
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
