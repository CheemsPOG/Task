/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.dto.request.UpsertChartTemplateRequest;
import com.task.chart.dto.response.ChartTemplateDto;
import com.task.chart.dto.response.ChartTemplateListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import java.util.List;

/**
 * Chart templates (design docs 136–139).
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
public interface ChartTemplateService {

	/**
	 * Lists chart template names for the current customer (name ascending).
	 *
	 * @return chart template list DTO
	 */
	List<ChartTemplateListItemDto> list();

	/**
	 * Registers or updates a template by customer + name (design doc 137).
	 *
	 * @param request name and content
	 * @return row update datetime as unix seconds
	 */
	SystemDatetimeResponse upsert(UpsertChartTemplateRequest request);

	/**
	 * Loads one template for the current customer (design doc 138).
	 *
	 * @param name path template name
	 * @return name and content
	 */
	ChartTemplateDto get(String name);

	/**
	 * Deletes one template for the current customer (design doc 139).
	 *
	 * @param name path template name
	 * @return system datetime (unix seconds)
	 */
	SystemDatetimeResponse delete(String name);
}
