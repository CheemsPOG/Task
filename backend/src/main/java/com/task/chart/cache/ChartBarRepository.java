/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC access to design-doc 121 {@code t_chart_*} warehouse tables.
 *
 * <p>Table names come only from {@link CacheNamespace} (never from request input).
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
@Repository
public class ChartBarRepository {

	private static final RowMapper<CachedChartBar> ROW_MAPPER = ChartBarRepository::mapRow;

	private final JdbcTemplate jdbcTemplate;

	/**
	 * Creates the repository.
	 *
	 * @param jdbcTemplate JDBC template
	 */
	public ChartBarRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Replaces all bars for one pair in a warehouse table.
	 *
	 * @param namespace table / cache mapping
	 * @param curpairCd currency pair CD
	 * @param bars bars to store
	 */
	public void replacePair(CacheNamespace namespace, String curpairCd, Collection<CachedChartBar> bars) {
		String table = namespace.tableName();
		jdbcTemplate.update("DELETE FROM " + table + " WHERE curpair_cd = ?", curpairCd);
		if (bars == null || bars.isEmpty()) {
			return;
		}
		java.util.LinkedHashMap<Long, CachedChartBar> unique = new java.util.LinkedHashMap<>();
		for (CachedChartBar bar : bars) {
			unique.put(bar.chartDatetimeSec(), bar);
		}
		String sql = insertSql(table);
		for (CachedChartBar bar : unique.values()) {
			jdbcTemplate.update(sql, ps -> bindBar(ps, bar));
		}
	}

	/**
	 * Upserts one bar (DELETE + INSERT — portable for H2 tests and Postgres).
	 *
	 * @param namespace table mapping
	 * @param bar bar to store
	 */
	public void upsert(CacheNamespace namespace, CachedChartBar bar) {
		String table = namespace.tableName();
		jdbcTemplate.update(
				"DELETE FROM " + table + " WHERE curpair_cd = ? AND chart_datetime = ?",
				bar.curpairCd(),
				bar.chartDatetimeSec());
		jdbcTemplate.update(insertSql(table), ps -> bindBar(ps, bar));
	}

	private static String insertSql(String table) {
		return "INSERT INTO " + table
				+ " (curpair_cd, chart_datetime, bid_open, bid_high, bid_low, bid_close,"
				+ " ask_open, ask_high, ask_low, ask_close, volume) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
	}

	/**
	 * Reads bars for a pair with optional from/to (unix seconds, inclusive).
	 *
	 * @param namespace table mapping
	 * @param curpairCd currency pair CD
	 * @param fromSec inclusive start, or {@code null}
	 * @param toSec inclusive end, or {@code null}
	 * @return ascending list
	 */
	public List<CachedChartBar> query(
			CacheNamespace namespace,
			String curpairCd,
			Long fromSec,
			Long toSec) {
		String table = namespace.tableName();
		if (fromSec == null && toSec == null) {
			return jdbcTemplate.query(
					"SELECT * FROM " + table + " WHERE curpair_cd = ? ORDER BY chart_datetime ASC",
					ROW_MAPPER,
					curpairCd);
		}
		if (fromSec != null && toSec != null) {
			if (fromSec > toSec) {
				return List.of();
			}
			return jdbcTemplate.query(
					"SELECT * FROM " + table
							+ " WHERE curpair_cd = ? AND chart_datetime >= ? AND chart_datetime <= ?"
							+ " ORDER BY chart_datetime ASC",
					ROW_MAPPER,
					curpairCd,
					fromSec,
					toSec);
		}
		if (fromSec != null) {
			return jdbcTemplate.query(
					"SELECT * FROM " + table
							+ " WHERE curpair_cd = ? AND chart_datetime >= ? ORDER BY chart_datetime ASC",
					ROW_MAPPER,
					curpairCd,
					fromSec);
		}
		return jdbcTemplate.query(
				"SELECT * FROM " + table
						+ " WHERE curpair_cd = ? AND chart_datetime <= ? ORDER BY chart_datetime ASC",
				ROW_MAPPER,
				curpairCd,
				toSec);
	}

	/**
	 * Latest chart datetime strictly before {@code fromSec}.
	 *
	 * @param namespace table mapping
	 * @param curpairCd currency pair CD
	 * @param fromSec from query parameter
	 * @return prior unix seconds, or {@code null}
	 */
	public Long nextTimeBefore(CacheNamespace namespace, String curpairCd, long fromSec) {
		String table = namespace.tableName();
		List<Long> rows = jdbcTemplate.query(
				"SELECT chart_datetime FROM " + table
						+ " WHERE curpair_cd = ? AND chart_datetime < ? ORDER BY chart_datetime DESC LIMIT 1",
				(rs, rowNum) -> rs.getLong(1),
				curpairCd,
				fromSec);
		return rows.isEmpty() ? null : rows.get(0);
	}

	/**
	 * Row count for a pair (diagnostics / tests).
	 *
	 * @param namespace table mapping
	 * @param curpairCd currency pair CD
	 * @return count
	 */
	public int size(CacheNamespace namespace, String curpairCd) {
		String table = namespace.tableName();
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + table + " WHERE curpair_cd = ?",
				Integer.class,
				curpairCd);
		return count == null ? 0 : count;
	}

	private static void bindBar(PreparedStatement ps, CachedChartBar bar) throws SQLException {
		ps.setString(1, bar.curpairCd());
		ps.setLong(2, bar.chartDatetimeSec());
		ps.setDouble(3, bar.bidOpen());
		ps.setDouble(4, bar.bidHigh());
		ps.setDouble(5, bar.bidLow());
		ps.setDouble(6, bar.bidClose());
		ps.setDouble(7, bar.askOpen());
		ps.setDouble(8, bar.askHigh());
		ps.setDouble(9, bar.askLow());
		ps.setDouble(10, bar.askClose());
		ps.setDouble(11, bar.volume());
	}

	private static CachedChartBar mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new CachedChartBar(
				rs.getString("curpair_cd"),
				rs.getLong("chart_datetime"),
				rs.getDouble("bid_open"),
				rs.getDouble("bid_high"),
				rs.getDouble("bid_low"),
				rs.getDouble("bid_close"),
				rs.getDouble("ask_open"),
				rs.getDouble("ask_high"),
				rs.getDouble("ask_low"),
				rs.getDouble("ask_close"),
				rs.getDouble("volume"));
	}
}
