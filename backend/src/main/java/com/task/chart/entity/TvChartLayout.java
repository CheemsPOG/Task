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
 * TV chart layout master row ({@code m_tv_chart_layout}).
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
@Entity
@Table(name = "m_tv_chart_layout")
public class TvChartLayout {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "customer_no", nullable = false)
	private long customerNo;

	@Column(name = "name", length = 64, nullable = false)
	private String name;

	@Column(name = "content", nullable = false)
	private String content;

	@Column(name = "ccypair_cd", length = 6, nullable = false)
	private String ccypairCd;

	@Column(name = "chart_type", length = 8, nullable = false)
	private String chartType;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected TvChartLayout() {
	}

	/**
	 * Creates a new layout row for register (design doc 127).
	 *
	 * @param customerNo token customer
	 * @param name layout name
	 * @param content layout JSON string
	 * @param ccypairCd currency pair CD
	 * @param chartType resolution
	 * @param updatedAt update timestamp
	 */
	public TvChartLayout(
			long customerNo,
			String name,
			String content,
			String ccypairCd,
			String chartType,
			Instant updatedAt) {
		this.customerNo = customerNo;
		this.name = name;
		this.content = content;
		this.ccypairCd = ccypairCd;
		this.chartType = chartType;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public long getCustomerNo() {
		return customerNo;
	}

	public String getName() {
		return name;
	}

	public String getContent() {
		return content;
	}

	public String getCcypairCd() {
		return ccypairCd;
	}

	public String getChartType() {
		return chartType;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * Applies update fields (design doc 128). Content comes from the request body
	 * (overview + TradingView save), not the doc update-conditions keep-existing quirk.
	 *
	 * @param name layout name
	 * @param content layout JSON string
	 * @param ccypairCd currency pair CD
	 * @param chartType resolution
	 * @param updatedAt new update timestamp
	 */
	public void applyUpdate(
			String name,
			String content,
			String ccypairCd,
			String chartType,
			Instant updatedAt) {
		this.name = name;
		this.content = content;
		this.ccypairCd = ccypairCd;
		this.chartType = chartType;
		this.updatedAt = updatedAt;
	}
}
