/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.dto.request.UpsertIndicatorTemplateRequest;
import com.task.chart.dto.response.IndicatorTemplateDto;
import com.task.chart.dto.response.IndicatorTemplateListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import java.util.List;

/**
 * Indicator templates (design docs 132–135).
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Add upsert/get/delete for 133–135</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
public interface IndicatorTemplateService {

	/**
	 * Lists indicator template names for the current customer (name ascending).
	 *
	 * @return indicator template list DTO
	 */
	List<IndicatorTemplateListItemDto> list();

	/**
	 * Registers or updates a template by customer + name (design doc 133).
	 *
	 * @param request name and content
	 * @return row update datetime as unix seconds
	 */
	SystemDatetimeResponse upsert(UpsertIndicatorTemplateRequest request);

	/**
	 * Loads one template for the current customer (design doc 134).
	 *
	 * @param name path template name
	 * @return name and content
	 */
	IndicatorTemplateDto get(String name);

	/**
	 * Deletes one template for the current customer (design doc 135).
	 *
	 * @param name path template name
	 * @return system datetime (unix seconds)
	 */
	SystemDatetimeResponse delete(String name);
}
