/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * Successful login payload with Bearer access token.
 *
 * <p>Extra versus design docs 120–139: local JWT stand-in for Peach SSO. {@code AuthServiceImpl}
 * returns this from {@code POST /api/auth/login}. {@code expiresIn} and {@code refreshExpiresIn}
 * are seconds. The refresh token itself is an HttpOnly cookie, not a field here. It is not the
 * refresh-endpoint body ({@link RefreshResponse}).
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
 *   <tr><td>1.1.0</td><td>2026/08/25</td><td>Task</td><td>Add refreshExpiresIn</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		long refreshExpiresIn) {
}
