/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * Chart layout payload for GET /api/layouts/{id} (design doc 129).
 *
 * <p>This JSON is one saved layout including widget {@code content}. {@code ChartLayoutServiceImpl}
 * maps {@code m_tv_chart_layout} for the JWT {@code customer_no}; another customer's id is 404.
 * {@code timestamp} is {@code updated_at} as unix seconds. It is not the list row
 * ({@link ChartLayoutListItemDto}, no content) and not the register/update id-only body.
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
public record ChartLayoutDto(
		long id,
		String name,
		long timestamp,
		String content) {
}
