/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.repository;

import com.task.chart.entity.TvChartTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TV chart template master access.
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
public interface TvChartTemplateRepository extends JpaRepository<TvChartTemplate, Long> {

	/**
	 * Templates for one customer, name ascending (design doc 136).
	 *
	 * @param customerNo token customer
	 * @return templates ordered by {@code name} ascending
	 */
	List<TvChartTemplate> findByCustomerNoOrderByNameAsc(long customerNo);

	/**
	 * One template for a customer by unique name (design docs 137–139).
	 *
	 * @param customerNo token customer
	 * @param name template name
	 * @return matching row, or empty
	 */
	Optional<TvChartTemplate> findByCustomerNoAndName(long customerNo, String name);
}
