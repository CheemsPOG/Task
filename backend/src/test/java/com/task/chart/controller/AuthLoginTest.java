/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.security.AuthCookieSupport;
import com.task.chart.support.TestAuthSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Auth login + Bearer gate (adjust plan Steps 2–3).
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
@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class Login {

		@Test
		void happyPathReturnsBearerToken() throws Exception {
			MvcResult result = mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"username":"demo","password":"demo"}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.accessToken").isString())
					.andExpect(jsonPath("$.tokenType").value("Bearer"))
					.andExpect(jsonPath("$.expiresIn").value(3600))
					.andExpect(jsonPath("$.refreshExpiresIn").value(86400))
					.andExpect(cookie().exists(AuthCookieSupport.REFRESH_COOKIE_NAME))
					.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("accessToken").asText()).isNotBlank();
		}

		@Test
		void badPasswordReturns401WithLocalizedBody() throws Exception {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"username":"demo","password":"wrong"}
									""")
							.header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.BAD_CREDENTIALS))
					.andExpect(jsonPath("$.message").value("Invalid username or password."));
		}

		@Test
		void blankBodyReturns422() throws Exception {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"username":"","password":""}
									"""))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class BearerGate {

		@Test
		void configWithoutTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/config").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.UNAUTHORIZED))
					.andExpect(jsonPath("$.message").value("Authentication is required."));
		}

		@Test
		void configWithInvalidTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/config").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.UNAUTHORIZED));
		}

		@Test
		void configWithDemoTokenReturns200() throws Exception {
			String bearer = TestAuthSupport.bearerDemo(mockMvc);
			mockMvc.perform(get("/api/config").header(HttpHeaders.AUTHORIZATION, bearer))
					.andExpect(status().isOk());
		}

		@Test
		void japaneseAcceptLanguageLocalizesUnauthorizedMessage() throws Exception {
			mockMvc.perform(get("/api/config").header(HttpHeaders.ACCEPT_LANGUAGE, "ja"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.UNAUTHORIZED))
					.andExpect(jsonPath("$.message").value("認証が必要です。"));
		}

		@Test
		void healthRemainsOpen() throws Exception {
			mockMvc.perform(get("/api/health")).andExpect(status().isOk());
		}
	}

	@Nested
	class RefreshAndLogout {

		@Test
		void refreshWithCookieReturnsNewAccessTokenAndRotatesCookie() throws Exception {
			MvcResult loginResult = TestAuthSupport.login(mockMvc, "demo", "demo");
			String refreshTokenId = TestAuthSupport.refreshCookieFromLogin(loginResult);
			String originalAccessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
					.get("accessToken")
					.asText();

			MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
							.cookie(TestAuthSupport.refreshCookie(refreshTokenId)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.accessToken").isString())
					.andExpect(jsonPath("$.tokenType").value("Bearer"))
					.andExpect(jsonPath("$.expiresIn").value(3600))
					.andExpect(cookie().exists(AuthCookieSupport.REFRESH_COOKIE_NAME))
					.andReturn();

			String newAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
					.get("accessToken")
					.asText();
			String rotatedRefreshTokenId = TestAuthSupport.refreshCookieFromLogin(refreshResult);

			assertThat(newAccessToken).isNotBlank().isNotEqualTo(originalAccessToken);
			assertThat(rotatedRefreshTokenId).isNotEqualTo(refreshTokenId);
		}

		@Test
		void refreshWithoutCookieReturns401() throws Exception {
			mockMvc.perform(post("/api/auth/refresh").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.UNAUTHORIZED))
					.andExpect(jsonPath("$.message").value("Authentication is required."));
		}

		@Test
		void logoutClearsCookieAndRevokesRefreshToken() throws Exception {
			MvcResult loginResult = TestAuthSupport.login(mockMvc, "demo", "demo");
			String refreshTokenId = TestAuthSupport.refreshCookieFromLogin(loginResult);
			Cookie refreshCookie = TestAuthSupport.refreshCookie(refreshTokenId);

			mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
					.andExpect(status().isOk())
					.andExpect(cookie().maxAge(AuthCookieSupport.REFRESH_COOKIE_NAME, 0));

			mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.UNAUTHORIZED));
		}
	}
}
