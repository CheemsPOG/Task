/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Local application user for JWT stand-in auth ({@code m_app_user}).
 *
 * <p>This JPA row stores demo logins ({@code demo} / {@code demo2}) so the chart can issue a local
 * JWT. Extra versus design docs 120–139 (those assume Peach SSO). {@code AuthServiceImpl} and
 * {@code AppUserSeedRunner} use it. {@code customer_no} is the tenant key for layouts and
 * templates. It is not a Peach S-01 account and is never returned as HTTP JSON.
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
@Entity
@Table(name = "m_app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "username", length = 64, nullable = false, unique = true)
	private String username;

	@Column(name = "password_hash", length = 100, nullable = false)
	private String passwordHash;

	@Column(name = "customer_no", nullable = false)
	private long customerNo;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AppUser() {
	}

	/**
	 * Creates a new enabled user row.
	 *
	 * @param username login name
	 * @param passwordHash BCrypt hash
	 * @param customerNo tenant customer number for layouts/templates
	 * @param createdAt create timestamp
	 */
	public AppUser(String username, String passwordHash, long customerNo, Instant createdAt) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.customerNo = customerNo;
		this.enabled = true;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public long getCustomerNo() {
		return customerNo;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
