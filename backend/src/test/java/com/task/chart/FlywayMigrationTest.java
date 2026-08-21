/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Asserts Flyway created and seeded {@code m_ccypairs} and {@code m_season}.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/20</td><td>Task</td><td>新規作成</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@SpringBootTest
class FlywayMigrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void seedsFiveCurrencyPairs() {
		List<String> codes = jdbcTemplate.queryForList(
				"SELECT ccypair_cd FROM m_ccypairs ORDER BY priority",
				String.class);
		assertThat(codes).containsExactly("USDJPY", "EURJPY", "EURUSD", "GBPUSD", "AUDUSD");
	}

	@Test
	void seedsStandardSeasonCoveringNow() {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT season_cd FROM m_season WHERE start_at <= CURRENT_TIMESTAMP AND end_at >= CURRENT_TIMESTAMP");
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).get("season_cd")).isEqualTo(2);
	}
}
