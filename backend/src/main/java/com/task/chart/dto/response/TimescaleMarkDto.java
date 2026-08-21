/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

import java.util.List;

/**
 * One timescale mark for GET /api/timescale_marks (design doc 126 + TradingView TimescaleMark).
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Doc 126 fields + library tooltip array</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
public record TimescaleMarkDto(
		String id,
		long time,
		String color,
		String label,
		List<String> tooltip,
		String labelFontColor) {
}
