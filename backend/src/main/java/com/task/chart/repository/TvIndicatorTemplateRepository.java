/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.repository;

import com.task.chart.entity.TvIndicatorTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TV indicator template master access.
 *
 * <p>Spring Data JPA for {@code m_tv_indicator_template} (design docs 132–135).
 * {@code IndicatorTemplateServiceImpl} lists by customer and loads by unique
 * {@code (customer_no, name)}. It is not chart-layout or chart-template access.
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
public interface TvIndicatorTemplateRepository extends JpaRepository<TvIndicatorTemplate, Long> {

	/**
	 * Templates for one customer, name ascending (design doc 132).
	 *
	 * @param customerNo token customer
	 * @return templates ordered by {@code name} ascending
	 */
	List<TvIndicatorTemplate> findByCustomerNoOrderByNameAsc(long customerNo);

	/**
	 * One template for a customer by unique name (design docs 133–135).
	 *
	 * @param customerNo token customer
	 * @param name template name
	 * @return matching row, or empty
	 */
	Optional<TvIndicatorTemplate> findByCustomerNoAndName(long customerNo, String name);
}
