/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.config;

import com.task.chart.entity.AppUser;
import com.task.chart.repository.AppUserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo local users if missing (Step 1 auth foundation).
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
public class AppUserSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AppUserSeedRunner.class);

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Creates the seed runner.
	 *
	 * @param appUserRepository user table
	 * @param passwordEncoder BCrypt encoder
	 */
	public AppUserSeedRunner(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		seedIfMissing("demo", "demo", 1L);
		seedIfMissing("demo2", "demo2", 2L);
	}

	private void seedIfMissing(String username, String rawPassword, long customerNo) {
		if (appUserRepository.existsByUsername(username)) {
			return;
		}

		String hash = passwordEncoder.encode(rawPassword);
		appUserRepository.save(new AppUser(username, hash, customerNo, Instant.now()));
		log.info("Seeded app user '{}' for customer_no={}", username, customerNo);
	}
}
