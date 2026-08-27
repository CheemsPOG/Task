/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.dto.request.RegisterChartLayoutRequest;
import com.task.chart.dto.response.ChartLayoutDto;
import com.task.chart.dto.response.ChartLayoutIdResponse;
import com.task.chart.dto.response.ChartLayoutListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import java.util.List;

/**
 * Chart layout register / update / get / list / delete (design docs 127–131).
 *
 * <p>Tenant CRUD on {@code m_tv_chart_layout} filtered by JWT {@code customer_no}. Symbol must exist
 * in {@code m_ccypairs}. {@link com.task.chart.controller.ChartLayoutController} is the HTTP caller.
 * Implemented by {@link com.task.chart.service.impl.ChartLayoutServiceImpl}. This is NOT chart
 * templates (136–139), NOT indicator templates (132–135), and NOT the widget.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Add get for doc 129</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/21</td><td>Task</td><td>Add list for doc 130</td></tr>
 *   <tr><td>1.3.0</td><td>2026/08/21</td><td>Task</td><td>Add delete for doc 131</td></tr>
 *   <tr><td>1.3.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.3.1
 */
public interface ChartLayoutService {

	/**
	 * Registers a new chart layout for the current customer.
	 *
	 * @param request layout name, content, symbol, resolution
	 * @return created layout id
	 */
	ChartLayoutIdResponse register(RegisterChartLayoutRequest request);

	/**
	 * Updates an existing chart layout for the current customer.
	 *
	 * @param idPath path parameter chart layout id (must be numeric)
	 * @param request layout name, content, symbol, resolution
	 * @return same layout id
	 */
	ChartLayoutIdResponse update(String idPath, RegisterChartLayoutRequest request);

	/**
	 * Loads one chart layout for the current customer.
	 *
	 * @param idPath path parameter chart layout id (must be numeric)
	 * @return layout DTO
	 */
	ChartLayoutDto get(String idPath);

	/**
	 * Lists chart layouts for the current customer (newest first).
	 *
	 * @return layout list DTO
	 */
	List<ChartLayoutListItemDto> list();

	/**
	 * Deletes one chart layout for the current customer.
	 *
	 * @param idPath path parameter chart layout id (must be numeric)
	 * @return system datetime (unix seconds)
	 */
	SystemDatetimeResponse delete(String idPath);
}
