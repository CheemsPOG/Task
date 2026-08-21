/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.dto.request.LoginRequest;
import com.task.chart.dto.response.LoginResponse;

/**
 * Local username/password login (JWT stand-in for S-01).
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
public interface AuthService {

	/**
	 * Authenticates and returns a Bearer access token.
	 *
	 * @param request username + password
	 * @return login response
	 */
	LoginResponse login(LoginRequest request);
}
