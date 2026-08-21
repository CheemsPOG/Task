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
public class JwtService {

	public static final String CLAIM_CUSTOMER_NO = "customer_no";

	private final SecretKey secretKey;
	private final long expirationMs;

	/**
	 * Creates the service from {@code app.jwt} properties.
	 *
	 * @param appProperties application config
	 */
	public JwtService(AppProperties appProperties) {
		AppProperties.Jwt jwt = appProperties.getJwt();
		this.secretKey = Keys.hmacShaKeyFor(jwt.getSecret().getBytes(StandardCharsets.UTF_8));
		this.expirationMs = jwt.getExpirationMs();
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
		Date expiry = new Date(now.getTime() + expirationMs);
		return Jwts.builder()
				.subject(username)
				.claim(CLAIM_CUSTOMER_NO, customerNo)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(secretKey)
				.compact();
	}

	/**
	 * @return configured token lifetime in milliseconds
	 */
	public long getExpirationMs() {
		return expirationMs;
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
		if (username == null || username.isBlank() || customerNo == null) {
			throw new JwtException("missing claims");
		}

		return new ChartPrincipal(username, customerNo.longValue());
	}
}
