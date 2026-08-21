/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.dto.request.LoginRequest;
import com.task.chart.dto.response.LoginResponse;
import com.task.chart.entity.AppUser;
import com.task.chart.exception.BadCredentialsAppException;
import com.task.chart.exception.ValidationException;
import com.task.chart.repository.AppUserRepository;
import com.task.chart.security.JwtService;
import com.task.chart.service.AuthService;
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@Service
public class AuthServiceImpl implements AuthService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	/**
	 * Creates the service.
	 *
	 * @param appUserRepository users
	 * @param passwordEncoder BCrypt
	 * @param jwtService token issuer
	 */
	public AuthServiceImpl(
			AppUserRepository appUserRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Override
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
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

		String token = jwtService.createToken(user.getUsername(), user.getCustomerNo());
		long expiresInSeconds = jwtService.getExpirationMs() / 1000L;
		return new LoginResponse(token, "Bearer", expiresInSeconds);
	}
}
