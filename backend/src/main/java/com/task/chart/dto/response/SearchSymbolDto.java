/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * One symbol-search hit for GET /api/search.
 *
 * <p>Design doc 124 returns this array so the widget search box can pick a pair.
 * {@code ChartDataServiceImpl} maps active {@code m_ccypairs} rows. JSON names are snake_case
 * ({@code full_name}) because that is the UDF contract. {@code symbol} is the 6-char CD;
 * {@code ticker} is the slashed display. It is not resolved symbol metadata
 * ({@link SymbolInfoDto}, design doc 123).
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
public record SearchSymbolDto(
		String symbol,
		String full_name,
		String ticker,
		String description,
		String exchange,
		String type) {
}
