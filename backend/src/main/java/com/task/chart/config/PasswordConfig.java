/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing beans for {@code m_app_user.password_hash}.
 *
 * <p>Exposes a BCrypt {@link PasswordEncoder} used by
 * {@link com.task.chart.service.impl.AuthServiceImpl} on login and by {@link AppUserSeedRunner}
 * when inserting {@code demo} / {@code demo2}. This is NOT JWT HMAC signing ({@code app.jwt.secret}),
 * NOT Peach S-01, and NOT the Python WS.
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
@Configuration
public class PasswordConfig {

	/**
	 * BCrypt encoder for {@code m_app_user.password_hash}.
	 *
	 * @return password encoder
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
