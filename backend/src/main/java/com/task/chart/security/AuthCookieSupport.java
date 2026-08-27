/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * HttpOnly refresh cookie helpers for the local S-01 stand-in.
 *
 * <p>Writes, reads, and clears cookie {@code chart_refresh_token} ({@code Path=/}, {@code SameSite=Lax}).
 * The cookie value is an opaque UUID, never a JWT; Redis key {@code peach:auth:refresh:{uuid}} holds the
 * session. {@link com.task.chart.service.impl.AuthServiceImpl} calls this on login, refresh, and logout.
 * This is NOT Peach S-01, NOT the access token in browser sessionStorage, and NOT the Python WS.
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
@Component
public class AuthCookieSupport {

	/** Cookie name for the opaque refresh token. */
	public static final String REFRESH_COOKIE_NAME = "chart_refresh_token";

	/**
	 * Sets the HttpOnly refresh cookie.
	 *
	 * @param response HTTP response
	 * @param tokenId opaque refresh id
	 * @param maxAgeSeconds cookie max-age in seconds
	 */
	public void setRefreshCookie(HttpServletResponse response, String tokenId, long maxAgeSeconds) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, tokenId)
				.httpOnly(true)
				.path("/")
				.sameSite("Lax")
				.maxAge(maxAgeSeconds)
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

	/**
	 * Clears the refresh cookie.
	 *
	 * @param response HTTP response
	 */
	public void clearRefreshCookie(HttpServletResponse response) {
		setRefreshCookie(response, "", 0L);
	}

	/**
	 * Reads the refresh token from the request cookies.
	 *
	 * @param request HTTP request
	 * @return opaque refresh id when present
	 */
	public Optional<String> readRefreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}

		for (Cookie cookie : cookies) {
			if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
				String value = cookie.getValue();

				// Blank cookie after logout must not count as a session.
				if (value != null && !value.isBlank()) {
					return Optional.of(value);
				}
			}
		}

		return Optional.empty();
	}
}
