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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Chart layouts (127–131)", description = "Register, update, get, list, delete saved layouts")
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
	@Operation(summary = "127 Register chart layout")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Created; body is { id }"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Unknown or deleted pair"),
			@ApiResponse(responseCode = "422", description = "Invalid body")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			content = @Content(
					examples = @ExampleObject(
							value = "{\"name\":\"My layout\",\"content\":\"{\\\"pane\\\":1}\","
									+ "\"symbol\":\"USDJPY\",\"resolution\":\"1D\"}")))
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
	@Operation(summary = "128 Update chart layout")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Updated; body is { id }"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Layout or pair not found"),
			@ApiResponse(responseCode = "422", description = "Non-numeric id or invalid body")
	})
	public ChartLayoutIdResponse update(
			@Parameter(description = "Numeric layout id") @PathVariable("id") String id,
			@RequestBody(required = false) RegisterChartLayoutRequest request) {
		return chartLayoutService.update(id, request);
	}

	/**
	 * Lists chart layouts for the current customer (design doc 130).
	 *
	 * @return layout list DTO (newest {@code updated_at} first)
	 */
	@GetMapping
	@Operation(summary = "130 Get chart layout list")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Layouts for this customer"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
	})
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
	@Operation(summary = "129 Get chart layout")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "id, name, timestamp, content"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Not found or other customer"),
			@ApiResponse(responseCode = "422", description = "Non-numeric id")
	})
	public ChartLayoutDto get(@Parameter(description = "Numeric layout id") @PathVariable("id") String id) {
		return chartLayoutService.get(id);
	}

	/**
	 * Deletes a chart layout (design doc 131).
	 *
	 * @param id path chart layout id (must be numeric)
	 * @return system datetime (unix seconds)
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "131 Delete chart layout")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Deleted; body is { t } unix seconds"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Not found or other customer"),
			@ApiResponse(responseCode = "422", description = "Non-numeric id")
	})
	public SystemDatetimeResponse delete(
			@Parameter(description = "Numeric layout id") @PathVariable("id") String id) {
		return chartLayoutService.delete(id);
	}
}
