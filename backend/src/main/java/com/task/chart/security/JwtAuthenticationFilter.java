/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

import com.task.chart.config.CustomerContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer} and sets SecurityContext + {@link CustomerContext}.
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
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	/**
	 * Creates the filter.
	 *
	 * @param jwtService JWT helper
	 */
	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		try {
			String header = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (header != null && header.startsWith(BEARER_PREFIX)) {
				String token = header.substring(BEARER_PREFIX.length()).trim();
				if (!token.isEmpty()) {
					authenticateBearer(token);
				}
			}

			filterChain.doFilter(request, response);
		} finally {
			CustomerContext.clear();
			SecurityContextHolder.clearContext();
		}
	}

	private void authenticateBearer(String token) {
		try {
			ChartPrincipal principal = jwtService.parseToken(token);
			setAuthentication(principal);
		} catch (JwtException | IllegalArgumentException ex) {
			SecurityContextHolder.clearContext();
			CustomerContext.clear();
		}
	}

	private void setAuthentication(ChartPrincipal principal) {
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		CustomerContext.set(principal.customerNo());
	}
}
