/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Currency pair master row ({@code m_ccypairs}).
 *
 * <p>This JPA row is the FX pair catalog for design docs 123 / 124 and extra {@code GET /curpairs}.
 * {@code ccypair_cd} is the 6-char PK ({@code USDJPY}). {@code priority} is the quote-stream id
 * exposed as {@code curpairCd}. {@code ChartDataServiceImpl}, {@code CurrencyPairServiceImpl}, and
 * {@code ChartLayoutServiceImpl} read it. It is never HTTP JSON and is not a bar warehouse row.
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
@Table(name = "m_ccypairs")
public class Ccypair {

	/** Soft-delete flag for live rows ({@code is_deleted = 0}). */
	public static final int ACTIVE = 0;

	@Id
	@Column(name = "ccypair_cd", length = 6, nullable = false)
	private String ccypairCd;

	@Column(name = "ccypair_jp", nullable = false)
	private String ccypairJp;

	@Column(name = "rate_unit", nullable = false)
	private int rateUnit;

	@Column(name = "is_deleted", nullable = false)
	private int isDeleted;

	@Column(name = "priority", nullable = false)
	private int priority;

	protected Ccypair() {
	}

	public String getCcypairCd() {
		return ccypairCd;
	}

	public String getCcypairJp() {
		return ccypairJp;
	}

	public int getRateUnit() {
		return rateUnit;
	}

	public int getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(int isDeleted) {
		this.isDeleted = isDeleted;
	}

	public int getPriority() {
		return priority;
	}
}
