/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.dto.request.LoginRequest;
import com.task.chart.dto.response.LoginResponse;
import com.task.chart.dto.response.RefreshResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Local username/password login (JWT stand-in for S-01).
 *
 * <p>Issues a 1h HS256 access JWT and a 1d opaque refresh UUID in Redis {@code peach:auth:refresh:*}
 * plus HttpOnly cookie {@code chart_refresh_token}. {@link com.task.chart.controller.AuthController}
 * calls login / refresh / logout. Implemented by {@link com.task.chart.service.impl.AuthServiceImpl}.
 * This is NOT Peach S-01 SSO, NOT the Python WS, and NOT the widget.
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
 *   <tr><td>1.1.0</td><td>2026/08/25</td><td>Task</td><td>Refresh cookie + logout</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public interface AuthService {

	/**
	 * Authenticates and returns a Bearer access token plus HttpOnly refresh cookie.
	 *
	 * @param request username + password
	 * @param response HTTP response for Set-Cookie
	 * @return login response
	 */
	LoginResponse login(LoginRequest request, HttpServletResponse response);

	/**
	 * Rotates the refresh cookie and returns a new access token.
	 *
	 * @param request HTTP request (refresh cookie)
	 * @param response HTTP response for rotated Set-Cookie
	 * @return refresh response
	 */
	RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response);

	/**
	 * Revokes the refresh token and clears the cookie (idempotent).
	 *
	 * @param request HTTP request (optional refresh cookie)
	 * @param response HTTP response for Clear-Cookie
	 */
	void logout(HttpServletRequest request, HttpServletResponse response);
}
