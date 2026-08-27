/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.exception;

/**
 * Missing or invalid refresh token / session.
 *
 * <p>{@link com.task.chart.service.impl.AuthServiceImpl} throws this when the HttpOnly refresh cookie
 * is absent or Redis {@code peach:auth:refresh:*} has expired. {@link GlobalExceptionHandler} maps it
 * to HTTP 401 {@code E_UNAUTHORIZED}. This is NOT {@link BadCredentialsAppException} (wrong password),
 * NOT filter-chain 401 from {@link com.task.chart.security.JsonUnauthorizedEntryPoint}, and NOT Peach
 * S-01.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/25</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public class UnauthorizedAppException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates the exception.
	 */
	public UnauthorizedAppException() {
		super("unauthorized");
	}
}
