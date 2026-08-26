/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.request.LoginRequest;
import com.task.chart.dto.response.LoginResponse;
import com.task.chart.dto.response.RefreshResponse;
import com.task.chart.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>OpenAPI operation docs</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/25</td><td>Task</td><td>Refresh + logout endpoints</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
 */
@RestController
@RequestMapping("/api/auth")
@Tag(
		name = "Auth",
		description = "Local JWT login with HttpOnly refresh cookie (mentor S-01 stand-in)")
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
	 * @param response HTTP response for Set-Cookie
	 * @return access token payload
	 */
	@PostMapping("/login")
	@SecurityRequirements
	@Operation(summary = "Login and get JWT", description = "Sets HttpOnly refresh cookie; access token in JSON.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Token issued"),
			@ApiResponse(responseCode = "401", description = "Bad username or password")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			content = @Content(
					examples = @ExampleObject(
							value = "{\"username\":\"demo\",\"password\":\"demo\"}")))
	public LoginResponse login(
			@RequestBody(required = false) LoginRequest request,
			HttpServletResponse response) {
		return authService.login(request, response);
	}

	/**
	 * Exchanges a valid refresh cookie for a new access token.
	 *
	 * @param request HTTP request (refresh cookie)
	 * @param response HTTP response for rotated Set-Cookie
	 * @return new access token payload
	 */
	@PostMapping("/refresh")
	@SecurityRequirements
	@Operation(summary = "Refresh access token", description = "Requires HttpOnly refresh cookie; no Bearer header.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "New access token issued"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid refresh cookie")
	})
	public RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
		return authService.refresh(request, response);
	}

	/**
	 * Revokes the refresh token and clears the cookie.
	 *
	 * @param request HTTP request (optional refresh cookie)
	 * @param response HTTP response for Clear-Cookie
	 */
	@PostMapping("/logout")
	@SecurityRequirements
	@Operation(summary = "Logout", description = "Revokes refresh token server-side and clears cookie.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Logged out")
	})
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		authService.logout(request, response);
	}
}
