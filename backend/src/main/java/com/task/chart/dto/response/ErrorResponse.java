/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * Error payload with stable {@code errorCode} and localized {@code message}.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Always include errorCode + localized message</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
public record ErrorResponse(String errorCode, String message) {

	/**
	 * Builds an error body.
	 *
	 * @param errorCode stable code
	 * @param message localized text
	 * @return response
	 */
	public static ErrorResponse of(String errorCode, String message) {
		return new ErrorResponse(errorCode, message);
	}
}
