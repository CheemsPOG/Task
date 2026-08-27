/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.exception;

import com.task.chart.constants.ErrorCodes;

/**
 * Request validation failure (HTTP 422, {@code CODE:30020}).
 *
 * <p>Services throw this for blank or illegal query/body fields on docs 120–139 and login. 
 * {@link GlobalExceptionHandler} maps it to JSON {@code errorCode} {@code CODE:30020}. This is NOT
 * {@link ResourceNotFoundException} (404), NOT auth 401, and NOT a widget-side validation error.
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
public class ValidationException extends RuntimeException {

	public ValidationException() {
		super(ErrorCodes.VALIDATION);
	}
}
