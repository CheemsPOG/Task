/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.config.AppProperties;
import com.task.chart.dto.request.LoginRequest;
import com.task.chart.dto.response.LoginResponse;
import com.task.chart.dto.response.RefreshResponse;
import com.task.chart.entity.AppUser;
import com.task.chart.exception.BadCredentialsAppException;
import com.task.chart.exception.UnauthorizedAppException;
import com.task.chart.exception.ValidationException;
import com.task.chart.repository.AppUserRepository;
import com.task.chart.security.AuthCookieSupport;
import com.task.chart.security.JwtService;
import com.task.chart.security.RefreshTokenSession;
import com.task.chart.security.RefreshTokenStore;
import com.task.chart.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService}.
 *
 * <p>Local S-01 stand-in: BCrypt check against {@code m_app_user}, then 1h access JWT plus Redis
 * {@code peach:auth:refresh:{uuid}} and cookie {@code chart_refresh_token}.
 * {@link com.task.chart.controller.AuthController} calls login / refresh / logout.
 *
 * <p><strong>NOT:</strong> not Peach SSO; not the Python WS; not the widget. The refresh
 * UUID is never a JSON field — only Redis + HttpOnly cookie. {@code login} is
 * {@code @Transactional(readOnly=true)} because it only reads {@code m_app_user};
 * a Redis write failure after that read has nothing to roll back.
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
 *   <tr><td>1.1.2</td><td>2026/08/31</td><td>Task</td><td>Review comments on 401 kinds</td></tr>
 *   <tr><td>1.1.3</td><td>2026/08/31</td><td>Task</td><td>Method overview Javadocs on helpers</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.3
 */
@Service
public class AuthServiceImpl implements AuthService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenStore refreshTokenStore;
	private final AuthCookieSupport authCookieSupport;
	private final long refreshExpirationSeconds;

	/**
	 * Creates the service.
	 *
	 * @param appUserRepository users
	 * @param passwordEncoder BCrypt
	 * @param jwtService token issuer
	 * @param refreshTokenStore Redis refresh store
	 * @param authCookieSupport refresh cookie helpers
	 * @param appProperties application config
	 */
	public AuthServiceImpl(
			AppUserRepository appUserRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			RefreshTokenStore refreshTokenStore,
			AuthCookieSupport authCookieSupport,
			AppProperties appProperties) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokenStore = refreshTokenStore;
		this.authCookieSupport = authCookieSupport;
		this.refreshExpirationSeconds = appProperties.getJwt().getRefreshExpirationMs() / 1000L;
	}

	/**
	 * Issues the access JWT. Refresh UUID goes to Redis + cookie only. Blank fields are
	 * 422 here ({@link LoginRequest} has no {@code @NotBlank}). Wrong password is 401
	 * {@code E_BAD_CREDENTIALS}, not the refresh-cookie 401.
	 */
	@Override
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request, HttpServletResponse response) {
		AppUser user = authenticate(request);
		String accessToken = jwtService.createToken(user.getUsername(), user.getCustomerNo());

		// Refresh UUID is only in Redis + HttpOnly cookie, never in the JSON body.
		String refreshTokenId = refreshTokenStore.issue(user.getUsername(), user.getCustomerNo());
		authCookieSupport.setRefreshCookie(response, refreshTokenId, refreshExpirationSeconds);
		return buildLoginResponse(accessToken);
	}

	/**
	 * Rotates the opaque refresh UUID. Missing/unknown cookie is 401
	 * {@code E_UNAUTHORIZED} (got past the filter; cookie was bad) — not
	 * {@code BadCredentialsAppException} (wrong password) and not the filter-chain 401.
	 */
	@Override
	public RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshTokenId = authCookieSupport.readRefreshToken(request)
				.orElseThrow(UnauthorizedAppException::new);

		// Steal-resistant: old Redis UUID dies before the new cookie is written.
		String rotatedTokenId = refreshTokenStore.rotate(refreshTokenId)
				.orElseThrow(UnauthorizedAppException::new);
		RefreshTokenSession session = refreshTokenStore.find(rotatedTokenId)
				.orElseThrow(UnauthorizedAppException::new);
		String accessToken = jwtService.createToken(session.username(), session.customerNo());
		authCookieSupport.setRefreshCookie(response, rotatedTokenId, refreshExpirationSeconds);
		return buildRefreshResponse(accessToken);
	}

	/**
	 * Idempotent: missing cookie still clears the browser cookie and returns 200.
	 * After this, {@link #refresh} must fail (rotated/revoked UUID is gone).
	 */
	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		authCookieSupport.readRefreshToken(request).ifPresent(refreshTokenStore::revoke);
		authCookieSupport.clearRefreshCookie(response);
	}

	/**
	 * {@link LoginRequest} has no bean-validation annotations — blanks are 422 here.
	 * Unknown, disabled, and wrong password all throw the same 401 so we do not leak
	 * whether the username exists.
	 */
	private AppUser authenticate(LoginRequest request) {
		if (request == null
				|| request.username() == null
				|| request.username().isBlank()
				|| request.password() == null
				|| request.password().isBlank()) {
			throw new ValidationException();
		}

		String username = request.username().trim();
		Optional<AppUser> found = appUserRepository.findByUsername(username);
		AppUser user = found.orElse(null);

		// Same 401 for unknown user, disabled user, and wrong password (no user-enum).
		if (user == null || !user.isEnabled()) {
			throw new BadCredentialsAppException();
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadCredentialsAppException();
		}
		return user;
	}

	/**
	 * JSON login body: access JWT + expiries only. Refresh UUID is never a field here
	 * (cookie + Redis). {@code refreshExpiresIn} is the TTL number, not the token.
	 */
	private LoginResponse buildLoginResponse(String accessToken) {
		long accessExpiresInSeconds = jwtService.getAccessExpirationMs() / 1000L;
		return new LoginResponse(accessToken, "Bearer", accessExpiresInSeconds, refreshExpirationSeconds);
	}

	/**
	 * Refresh JSON is a new access JWT only — still no refresh UUID in the body.
	 */
	private RefreshResponse buildRefreshResponse(String accessToken) {
		long accessExpiresInSeconds = jwtService.getAccessExpirationMs() / 1000L;
		return new RefreshResponse(accessToken, "Bearer", accessExpiresInSeconds);
	}
}
