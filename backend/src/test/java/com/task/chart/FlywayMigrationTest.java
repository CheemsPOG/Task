/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.chart.entity.AppUser;
import com.task.chart.repository.AppUserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Asserts Flyway created and seeded chart masters plus local app users.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Assert V7 m_app_user + demo seeds</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
@SpringBootTest
class FlywayMigrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

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

	@Test
	void seedsUsdJpyDailyMarks() {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT id, color, label FROM m_tv_mark WHERE ccypair_cd = 'USDJPY' AND resolution = '1D' ORDER BY mark_at");
		assertThat(rows).hasSize(3);
		assertThat(rows.get(0).get("id")).isEqualTo("m1");
		assertThat(rows.get(0).get("color")).isEqualTo("green");
		assertThat(rows.get(1).get("label")).isEqualTo("S");
	}

	@Test
	void seedsUsdJpyDailyTimescaleMarks() {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT id, label, tooltip FROM m_tv_timescale_mark WHERE ccypair_cd = 'USDJPY' AND resolution = '1D' ORDER BY timescale_mark_at");
		assertThat(rows).hasSize(3);
		assertThat(rows.get(0).get("id")).isEqualTo("tm1");
		assertThat(rows.get(0).get("label")).isEqualTo("B");
		assertThat(rows.get(1).get("tooltip")).isEqualTo("Sell event");
	}

	@Test
	void createsChartLayoutTable() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = 'm_tv_chart_layout'",
				Integer.class);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void createsIndicatorTemplateTable() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = 'm_tv_indicator_template'",
				Integer.class);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void createsAppUserTable() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = 'm_app_user'",
				Integer.class);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void createsAllThirteenChartWarehouseTables() {
		List<String> expected = List.of(
				"t_chart_1",
				"t_chart_60",
				"t_chart_300",
				"t_chart_600",
				"t_chart_900",
				"t_chart_1800",
				"t_chart_3600",
				"t_chart_7200",
				"t_chart_14400",
				"t_chart_28800",
				"t_chart_day",
				"t_chart_week",
				"t_chart_month");
		for (String table : expected) {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = ?",
					Integer.class,
					table);
			assertThat(count).as(table).isEqualTo(1);
		}
	}

	@Test
	void seedsDemoUsersWithBcryptPasswords() {
		AppUser demo = appUserRepository.findByUsername("demo").orElseThrow();
		assertThat(demo.getCustomerNo()).isEqualTo(1L);
		assertThat(demo.isEnabled()).isTrue();
		assertThat(passwordEncoder.matches("demo", demo.getPasswordHash())).isTrue();
		assertThat(demo.getPasswordHash()).startsWith("$2");

		AppUser demo2 = appUserRepository.findByUsername("demo2").orElseThrow();
		assertThat(demo2.getCustomerNo()).isEqualTo(2L);
		assertThat(passwordEncoder.matches("demo2", demo2.getPasswordHash())).isTrue();
	}
}
