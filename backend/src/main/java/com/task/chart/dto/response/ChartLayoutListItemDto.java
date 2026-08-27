/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * One row in GET /api/layouts list (design doc 130).
 *
 * <p>This is a metadata-only list entry (no widget {@code content}) for the JWT customer's layouts.
 * {@code ChartLayoutServiceImpl} maps {@code m_tv_chart_layout} newest {@code updated_at} first.
 * {@code symbol} is the 6-char pair CD; {@code resolution} is {@code chart_type}. It is not the
 * full GET-by-id payload ({@link ChartLayoutDto}).
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/21</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public record ChartLayoutListItemDto(
		long id,
		String name,
		String resolution,
		String symbol,
		long timestamp) {
}
