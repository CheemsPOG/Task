/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

import com.task.chart.config.AppProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed opaque refresh tokens ({@code peach:auth:refresh:*}).
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@Component
public class RefreshTokenStore {

	private static final String KEY_PREFIX = "peach:auth:refresh:";
	private static final String VALUE_SEPARATOR = "|";

	private final StringRedisTemplate redis;
	private final Duration refreshTtl;

	/**
	 * Creates the store.
	 *
	 * @param redis string Redis template
	 * @param appProperties application config
	 */
	public RefreshTokenStore(StringRedisTemplate redis, AppProperties appProperties) {
		this.redis = redis;
		this.refreshTtl = Duration.ofMillis(appProperties.getJwt().getRefreshExpirationMs());
	}

	/**
	 * Issues a new opaque refresh token for the user.
	 *
	 * @param username login name
	 * @param customerNo tenant id
	 * @return opaque token id
	 */
	public String issue(String username, long customerNo) {
		String tokenId = UUID.randomUUID().toString();
		String value = username + VALUE_SEPARATOR + customerNo;
		redis.opsForValue().set(key(tokenId), value, refreshTtl);
		return tokenId;
	}

	/**
	 * Looks up a refresh session by opaque id.
	 *
	 * @param tokenId opaque refresh id
	 * @return session when present and not expired
	 */
	public Optional<RefreshTokenSession> find(String tokenId) {
		if (tokenId == null || tokenId.isBlank()) {
			return Optional.empty();
		}
		String value = redis.opsForValue().get(key(tokenId));
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		int separator = value.indexOf(VALUE_SEPARATOR);
		if (separator <= 0 || separator >= value.length() - 1) {
			return Optional.empty();
		}
		String username = value.substring(0, separator);
		long customerNo = Long.parseLong(value.substring(separator + 1));
		return Optional.of(new RefreshTokenSession(username, customerNo));
	}

	/**
	 * Revokes a refresh token.
	 *
	 * @param tokenId opaque refresh id
	 */
	public void revoke(String tokenId) {
		if (tokenId == null || tokenId.isBlank()) {
			return;
		}
		redis.delete(key(tokenId));
	}

	/**
	 * Rotates a refresh token: revokes the old id and issues a new one for the same user.
	 *
	 * @param oldTokenId current opaque id
	 * @return new opaque id, or empty when the old token is invalid
	 */
	public Optional<String> rotate(String oldTokenId) {
		Optional<RefreshTokenSession> session = find(oldTokenId);
		if (session.isEmpty()) {
			return Optional.empty();
		}
		revoke(oldTokenId);
		RefreshTokenSession found = session.get();
		return Optional.of(issue(found.username(), found.customerNo()));
	}

	private static String key(String tokenId) {
		return KEY_PREFIX + tokenId;
	}
}
