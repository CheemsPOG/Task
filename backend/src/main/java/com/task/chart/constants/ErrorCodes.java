/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.constants;

/**
 * Error codes from the overview design documents.
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
public final class ErrorCodes {

	public static final String VALIDATION = "CODE:30020";
	public static final String NOT_FOUND = "CODE:30404";
	public static final String SERVER = "E_SERVER";
	public static final String SERVER_MESSAGE = "システムエラーが発生しました。";

	private ErrorCodes() {
	}
}
