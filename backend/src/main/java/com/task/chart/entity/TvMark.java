/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * TV mark master row ({@code m_tv_mark}).
 *
 * <p>This JPA row is a demo pin on the price series for design doc 125. Flyway V3 seeds USDJPY 1D
 * rows. There is no {@code customer_no} — marks are global. {@code ChartDataServiceImpl} maps it
 * to {@code MarkDto}. {@code mark_at} is unix seconds. It is never HTTP JSON and is not a
 * timescale mark ({@code m_tv_timescale_mark}).
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
@Table(name = "m_tv_mark")
public class TvMark {

	@Id
	@Column(name = "id", length = 32, nullable = false)
	private String id;

	@Column(name = "ccypair_cd", length = 6, nullable = false)
	private String ccypairCd;

	@Column(name = "resolution", length = 8, nullable = false)
	private String resolution;

	@Column(name = "mark_at", nullable = false)
	private long markAt;

	@Column(name = "color", length = 32, nullable = false)
	private String color;

	@Column(name = "label", length = 8, nullable = false)
	private String label;

	@Column(name = "mark_text", nullable = false)
	private String markText;

	protected TvMark() {
	}

	public String getId() {
		return id;
	}

	public String getCcypairCd() {
		return ccypairCd;
	}

	public String getResolution() {
		return resolution;
	}

	public long getMarkAt() {
		return markAt;
	}

	public String getColor() {
		return color;
	}

	public String getLabel() {
		return label;
	}

	public String getMarkText() {
		return markText;
	}
}
