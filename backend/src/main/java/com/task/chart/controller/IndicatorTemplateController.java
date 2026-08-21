/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.response.IndicatorTemplateListItemDto;
import com.task.chart.service.IndicatorTemplateService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Indicator template REST endpoints (design docs 132+).
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
@RestController
@RequestMapping("/api/indicator-templates")
public class IndicatorTemplateController {

	private final IndicatorTemplateService indicatorTemplateService;

	/**
	 * Creates the controller.
	 *
	 * @param indicatorTemplateService indicator template service
	 */
	public IndicatorTemplateController(IndicatorTemplateService indicatorTemplateService) {
		this.indicatorTemplateService = indicatorTemplateService;
	}

	/**
	 * Lists indicator templates for the current customer (design doc 132).
	 *
	 * @return indicator template list DTO (name only, sorted by name)
	 */
	@GetMapping
	public List<IndicatorTemplateListItemDto> list() {
		return indicatorTemplateService.list();
	}
}
