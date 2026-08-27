/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

import com.task.chart.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Creates and parses local demo JWTs (S-01 stand-in).
 *
 * <p>HS256 access tokens: {@code sub} is username, claim {@code customer_no} is the tenant, lifetime
 * 1h from {@code app.jwt}. {@link com.task.chart.service.impl.AuthServiceImpl} issues tokens;
 * {@link JwtAuthenticationFilter} parses them. This is NOT Peach S-01 SSO, NOT the opaque refresh
 * UUID in Redis, and NOT the Python WS.
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
@Service
public class JwtService {

	public static final String CLAIM_CUSTOMER_NO = "customer_no";

	private final SecretKey secretKey;
	private final long accessExpirationMs;

	/**
	 * Creates the service from {@code app.jwt} properties.
	 *
	 * @param appProperties application config
	 */
	public JwtService(AppProperties appProperties) {
		AppProperties.Jwt jwt = appProperties.getJwt();
		this.secretKey = Keys.hmacShaKeyFor(jwt.getSecret().getBytes(StandardCharsets.UTF_8));
		this.accessExpirationMs = jwt.getAccessExpirationMs();
	}

	/**
	 * Builds a signed access token.
	 *
	 * @param username login name
	 * @param customerNo tenant id
	 * @return compact JWT
	 */
	public String createToken(String username, long customerNo) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + accessExpirationMs);
		return Jwts.builder()
				.subject(username)
				.claim(CLAIM_CUSTOMER_NO, customerNo)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(secretKey)
				.compact();
	}

	/**
	 * @return configured access token lifetime in milliseconds
	 */
	public long getAccessExpirationMs() {
		return accessExpirationMs;
	}

	/**
	 * Parses and validates a JWT.
	 *
	 * @param token compact JWT (without Bearer prefix)
	 * @return principal
	 * @throws JwtException if invalid or expired
	 */
	public ChartPrincipal parseToken(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		String username = claims.getSubject();
		Number customerNo = claims.get(CLAIM_CUSTOMER_NO, Number.class);

		// Layouts/templates need both identity and tenant; a token with only sub is rejected.
		if (username == null || username.isBlank() || customerNo == null) {
			throw new JwtException("missing claims");
		}

		return new ChartPrincipal(username, customerNo.longValue());
	}
}
