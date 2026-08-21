/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.request.RegisterChartLayoutRequest;
import com.task.chart.dto.response.ChartLayoutDto;
import com.task.chart.dto.response.ChartLayoutIdResponse;
import com.task.chart.dto.response.ChartLayoutListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import com.task.chart.service.ChartLayoutService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chart layout REST endpoints (design docs 127+).
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
@RequestMapping("/api/layouts")
public class ChartLayoutController {

	private final ChartLayoutService chartLayoutService;

	/**
	 * Creates the controller.
	 *
	 * @param chartLayoutService layout service
	 */
	public ChartLayoutController(ChartLayoutService chartLayoutService) {
		this.chartLayoutService = chartLayoutService;
	}

	/**
	 * Registers a chart layout (design doc 127).
	 *
	 * @param request name, content, symbol, resolution
	 * @return created layout id
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ChartLayoutIdResponse register(@RequestBody(required = false) RegisterChartLayoutRequest request) {
		return chartLayoutService.register(request);
	}

	/**
	 * Updates a chart layout (design doc 128).
	 *
	 * @param id path chart layout id (must be numeric)
	 * @param request name, content, symbol, resolution
	 * @return same layout id
	 */
	@PutMapping("/{id}")
	public ChartLayoutIdResponse update(
			@PathVariable("id") String id,
			@RequestBody(required = false) RegisterChartLayoutRequest request) {
		return chartLayoutService.update(id, request);
	}

	/**
	 * Lists chart layouts for the current customer (design doc 130).
	 *
	 * @return layout list DTO (newest {@code updated_at} first)
	 */
	@GetMapping
	public List<ChartLayoutListItemDto> list() {
		return chartLayoutService.list();
	}

	/**
	 * Loads a chart layout (design doc 129).
	 *
	 * @param id path chart layout id (must be numeric)
	 * @return layout DTO
	 */
	@GetMapping("/{id}")
	public ChartLayoutDto get(@PathVariable("id") String id) {
		return chartLayoutService.get(id);
	}

	/**
	 * Deletes a chart layout (design doc 131).
	 *
	 * @param id path chart layout id (must be numeric)
	 * @return system datetime (unix seconds)
	 */
	@DeleteMapping("/{id}")
	public SystemDatetimeResponse delete(@PathVariable("id") String id) {
		return chartLayoutService.delete(id);
	}
}
