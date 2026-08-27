/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.repository;

import com.task.chart.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Local application user access ({@code m_app_user}).
 *
 * <p>Spring Data JPA for demo logins. {@code AuthServiceImpl} looks up by username on login;
 * {@code AppUserSeedRunner} uses {@code existsByUsername} before inserting {@code demo} /
 * {@code demo2}. Extra versus design docs 120–139. It is not a Peach SSO user store and not the
 * JDBC bar warehouse.
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
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	/**
	 * Finds a user by login name.
	 *
	 * @param username login name
	 * @return user if present
	 */
	Optional<AppUser> findByUsername(String username);

	/**
	 * Whether a login name already exists.
	 *
	 * @param username login name
	 * @return true if taken
	 */
	boolean existsByUsername(String username);
}
