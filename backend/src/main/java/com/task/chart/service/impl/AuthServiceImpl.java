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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
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

	@Override
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request, HttpServletResponse response) {
		AppUser user = authenticate(request);
		String accessToken = jwtService.createToken(user.getUsername(), user.getCustomerNo());
		String refreshTokenId = refreshTokenStore.issue(user.getUsername(), user.getCustomerNo());
		authCookieSupport.setRefreshCookie(response, refreshTokenId, refreshExpirationSeconds);
		return buildLoginResponse(accessToken);
	}

	@Override
	public RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshTokenId = authCookieSupport.readRefreshToken(request)
				.orElseThrow(UnauthorizedAppException::new);
		String rotatedTokenId = refreshTokenStore.rotate(refreshTokenId)
				.orElseThrow(UnauthorizedAppException::new);
		RefreshTokenSession session = refreshTokenStore.find(rotatedTokenId)
				.orElseThrow(UnauthorizedAppException::new);
		String accessToken = jwtService.createToken(session.username(), session.customerNo());
		authCookieSupport.setRefreshCookie(response, rotatedTokenId, refreshExpirationSeconds);
		return buildRefreshResponse(accessToken);
	}

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		authCookieSupport.readRefreshToken(request).ifPresent(refreshTokenStore::revoke);
		authCookieSupport.clearRefreshCookie(response);
	}

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
		if (user == null || !user.isEnabled()) {
			throw new BadCredentialsAppException();
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadCredentialsAppException();
		}
		return user;
	}

	private LoginResponse buildLoginResponse(String accessToken) {
		long accessExpiresInSeconds = jwtService.getAccessExpirationMs() / 1000L;
		return new LoginResponse(accessToken, "Bearer", accessExpiresInSeconds, refreshExpirationSeconds);
	}

	private RefreshResponse buildRefreshResponse(String accessToken) {
		long accessExpiresInSeconds = jwtService.getAccessExpirationMs() / 1000L;
		return new RefreshResponse(accessToken, "Bearer", accessExpiresInSeconds);
	}
}
