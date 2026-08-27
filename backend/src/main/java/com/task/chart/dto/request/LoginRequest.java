/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Login body for {@code POST /api/auth/login}.
 *
 * <p>This JSON carries the local demo username and password so the chart can obtain a JWT.
 * It is extra versus design docs 120–139, which assume Peach SSO. {@code AuthController}
 * deserializes it and {@code AuthServiceImpl} checks BCrypt against {@code m_app_user}.
 * It is not a Peach S-01 token exchange and does not include a refresh token.
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public record LoginRequest(
		@Schema(description = "Login name", example = "demo") String username,
		@Schema(description = "Password", example = "demo") String password) {
}
