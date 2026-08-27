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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * TV chart template master row ({@code m_tv_chart_templates}).
 *
 * <p>This JPA row is a TradingView chart-style preset (theme/colors) for design docs 136–139.
 * Unique on {@code (customer_no, name)}. {@code ChartTemplateServiceImpl} upserts, lists, gets,
 * and deletes it. The table name is plural in the spec. It is never HTTP JSON, not a saved layout
 * (docs 127–131), and not a study template (docs 132–135).
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/24</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
@Entity
@Table(
		name = "m_tv_chart_templates",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_m_tv_chart_templates_customer_name",
				columnNames = { "customer_no", "name" }))
public class TvChartTemplate {

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

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected TvChartTemplate() {
	}

	/**
	 * Creates a new chart template row.
	 *
	 * @param customerNo token customer
	 * @param name template name
	 * @param content template JSON string
	 * @param updatedAt update timestamp
	 */
	public TvChartTemplate(long customerNo, String name, String content, Instant updatedAt) {
		this.customerNo = customerNo;
		this.name = name;
		this.content = content;
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

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * Updates content only (design doc 137 update-conditions). Name and customer stay.
	 *
	 * @param content template JSON string
	 * @param updatedAt new update timestamp
	 */
	public void applyContent(String content, Instant updatedAt) {
		this.content = content;
		this.updatedAt = updatedAt;
	}
}
