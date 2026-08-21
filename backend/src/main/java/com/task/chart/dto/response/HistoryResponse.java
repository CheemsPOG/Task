/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

import java.util.List;

/**
 * Historical bar payload for GET /api/history.
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
