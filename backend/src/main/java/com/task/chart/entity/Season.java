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
 * Season master row ({@code m_season}).
 *
 * <p>This JPA row is a DST vs standard time window so design doc 123 can pick the widget
 * {@code session} string ({@code timeSummer} / {@code timeWinter} from yml).
 * {@code ChartDataServiceImpl.currentSession} reads the row covering now. It is not a holiday
 * calendar and is never HTTP JSON.
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
@Entity
@Table(name = "m_season")
public class Season {

	/** Peach season code that selects the daylight-saving session string (design doc 123). */
	public static final int DAYLIGHT_SAVING = 1;

	/** Peach season code that selects the standard session string (design doc 123). */
	public static final int STANDARD = 2;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "season_cd", nullable = false)
	private int seasonCd;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	protected Season() {
	}

	public Long getId() {
		return id;
	}

	public int getSeasonCd() {
		return seasonCd;
	}

	public void setSeasonCd(int seasonCd) {
		this.seasonCd = seasonCd;
	}

	public Instant getStartAt() {
		return startAt;
	}

	public Instant getEndAt() {
		return endAt;
	}

	public void setEndAt(Instant endAt) {
		this.endAt = endAt;
	}
}
