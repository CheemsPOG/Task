/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.security.AuthCookieSupport;
import com.task.chart.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * MockMvc helpers for JWT Bearer auth in integration tests.
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
public final class TestAuthSupport {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private TestAuthSupport() {
	}

	/**
	 * Logs in as {@code demo} / {@code demo} (customer 1) and returns a Bearer header value.
	 *
	 * @param mockMvc mock MVC
	 * @return {@code Bearer <token>}
	 * @throws Exception on login failure
	 */
	public static String bearerDemo(MockMvc mockMvc) throws Exception {
		return bearerLogin(mockMvc, "demo", "demo");
	}

	/**
	 * Logs in as {@code demo2} / {@code demo2} (customer 2).
	 *
	 * @param mockMvc mock MVC
	 * @return {@code Bearer <token>}
	 * @throws Exception on login failure
	 */
	public static String bearerDemo2(MockMvc mockMvc) throws Exception {
		return bearerLogin(mockMvc, "demo2", "demo2");
	}

	/**
	 * Logs in and returns {@code Bearer <token>}.
	 *
	 * @param mockMvc mock MVC
	 * @param username username
	 * @param password password
	 * @return authorization header value
	 * @throws Exception on login failure
	 */
	public static String bearerLogin(MockMvc mockMvc, String username, String password) throws Exception {
		MvcResult result = login(mockMvc, username, password);
		String token = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString())
				.get("accessToken")
				.asText();
		return "Bearer " + token;
	}

	/**
	 * Logs in and returns the raw MockMvc result (access token + Set-Cookie).
	 *
	 * @param mockMvc mock MVC
	 * @param username username
	 * @param password password
	 * @return login response
	 * @throws Exception on login failure
	 */
	public static MvcResult login(MockMvc mockMvc, String username, String password) throws Exception {
		String body = OBJECT_MAPPER.createObjectNode()
				.put("username", username)
				.put("password", password)
				.toString();
		return mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn();
	}

	/**
	 * Extracts the refresh cookie value from a login response.
	 *
	 * @param loginResult login MockMvc result
	 * @return refresh token id
	 */
	public static String refreshCookieFromLogin(MvcResult loginResult) {
		Cookie cookie = loginResult.getResponse().getCookie(AuthCookieSupport.REFRESH_COOKIE_NAME);
		if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
			throw new IllegalStateException("Missing refresh cookie on login response");
		}
		return cookie.getValue();
	}

	/**
	 * Builds a refresh cookie for MockMvc requests.
	 *
	 * @param refreshTokenId opaque refresh id
	 * @return servlet cookie
	 */
	public static Cookie refreshCookie(String refreshTokenId) {
		return new Cookie(AuthCookieSupport.REFRESH_COOKIE_NAME, refreshTokenId);
	}

	/**
	 * Mints a Bearer token for an arbitrary customer (no DB user required).
	 *
	 * @param jwtService JWT service
	 * @param username subject
	 * @param customerNo tenant id
	 * @return {@code Bearer <token>}
	 */
	public static String bearerForCustomer(JwtService jwtService, String username, long customerNo) {
		return "Bearer " + jwtService.createToken(username, customerNo);
	}

	/**
	 * Request post-processor that adds an Authorization header.
	 *
	 * @param bearerHeader full {@code Bearer …} value
	 * @return post processor
	 */
	public static RequestPostProcessor withBearer(String bearerHeader) {
		return request -> {
			request.addHeader(HttpHeaders.AUTHORIZATION, bearerHeader);
			return request;
		};
	}
}
