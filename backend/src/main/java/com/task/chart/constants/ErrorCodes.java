/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.constants;

/**
 * Error codes from the overview design documents and auth stand-in.
 *
 * <p>Wire {@code errorCode} strings: docs 120–139 use {@code CODE:30020} / {@code CODE:30404};
 * local auth uses {@code E_UNAUTHORIZED} / {@code E_BAD_CREDENTIALS} / {@code E_SERVER}. {@code MSG_*}
 * keys map to {@code messages.properties}. {@link com.task.chart.exception.GlobalExceptionHandler}
 * and {@link com.task.chart.security.JsonUnauthorizedEntryPoint} send these. This is NOT Peach
 * production error catalogs and NOT the widget.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Auth error codes + message keys</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public final class ErrorCodes {

	public static final String VALIDATION = "CODE:30020";
	public static final String NOT_FOUND = "CODE:30404";
	public static final String SERVER = "E_SERVER";
	public static final String UNAUTHORIZED = "E_UNAUTHORIZED";
	public static final String BAD_CREDENTIALS = "E_BAD_CREDENTIALS";

	public static final String MSG_VALIDATION = "error.validation";
	public static final String MSG_NOT_FOUND = "error.not_found";
	public static final String MSG_SERVER = "error.server";
	public static final String MSG_UNAUTHORIZED = "error.unauthorized";
	public static final String MSG_BAD_CREDENTIALS = "error.bad_credentials";
	public static final String MSG_BAD_REQUEST = "error.bad_request";

	/** @deprecated use MessageSource key {@link #MSG_SERVER} */
	@Deprecated
	public static final String SERVER_MESSAGE = "システムエラーが発生しました。";

	private ErrorCodes() {
	}
}
