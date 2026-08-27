/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.exception;

import com.task.chart.constants.ErrorCodes;

/**
 * Unexpected server failure (HTTP 500, {@code E_SERVER}).
 *
 * <p>Thrown when {@link com.task.chart.security.CustomerContext} is unset on a tenant API, or when
 * {@code m_season} has no row covering now (doc 123 session). {@link GlobalExceptionHandler} maps it
 * to {@code E_SERVER}. This is NOT a Redis outage mapped here, NOT Peach production errors, and NOT
 * the widget.
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
public class ServerErrorException extends RuntimeException {

	public ServerErrorException() {
		super(ErrorCodes.SERVER_MESSAGE);
	}
}
