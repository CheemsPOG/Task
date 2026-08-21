/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.security.JwtService;
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
		String body = OBJECT_MAPPER.createObjectNode()
				.put("username", username)
				.put("password", password)
				.toString();
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn();
		String token = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString())
				.get("accessToken")
				.asText();
		return "Bearer " + token;
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
