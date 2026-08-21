/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.request.LoginRequest;
import com.task.chart.dto.response.LoginResponse;
import com.task.chart.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local auth endpoints (JWT stand-in for S-01).
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
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	/**
	 * Creates the controller.
	 *
	 * @param authService auth service
	 */
	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * Logs in with username/password and returns a Bearer token.
	 *
	 * @param request credentials
	 * @return access token payload
	 */
	@PostMapping("/login")
	public LoginResponse login(@RequestBody(required = false) LoginRequest request) {
		return authService.login(request);
	}
}
