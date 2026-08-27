/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.exception;

import com.task.chart.constants.ErrorCodes;

/**
 * Missing resource (HTTP 404, {@code CODE:30404}).
 *
 * <p>Thrown when a layout, template, or {@code m_ccypairs} row is missing, deleted, or owned by
 * another {@code customer_no}. {@link GlobalExceptionHandler} maps it to JSON {@code CODE:30404}.
 * Marks (docs 125/126) return empty lists instead of this. This is NOT 422 validation, NOT the
 * Python WS, and NOT the widget.
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
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException() {
		super(ErrorCodes.NOT_FOUND);
	}
}
