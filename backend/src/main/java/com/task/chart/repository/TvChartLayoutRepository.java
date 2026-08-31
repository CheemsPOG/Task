/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.repository;

import com.task.chart.entity.TvChartLayout;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TV chart layout master access.
 *
 * <p>Spring Data JPA for {@code m_tv_chart_layout} (design docs 127–131).
 * {@code ChartLayoutServiceImpl} lists by JWT {@code customer_no} and uses inherited
 * {@code findById} / {@code save} / {@code delete} for the other verbs. Get/update/delete use
 * inherited {@code findById}; the service then checks {@code customer_no} so another tenant's
 * id is 404. Do not add an unscoped finder that returns layout content.
 *
 * <p>Not a chart-template repository and not JDBC bars.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Add list by customer for doc 130</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public interface TvChartLayoutRepository extends JpaRepository<TvChartLayout, Long> {

	/**
	 * Layouts for one customer, newest update first (design doc 130).
	 *
	 * @param customerNo token customer
	 * @return layouts ordered by {@code updated_at} descending
	 */
	List<TvChartLayout> findByCustomerNoOrderByUpdatedAtDesc(long customerNo);
}
