/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.request.UpsertChartTemplateRequest;
import com.task.chart.dto.response.ChartTemplateDto;
import com.task.chart.dto.response.ChartTemplateListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import com.task.chart.service.ChartTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chart template HTTP API (design docs 136–139). HTTP only; no SQL.
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
 *   <tr><td>1.1.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
@RestController
@RequestMapping("/api/chart-templates")
@Tag(name = "Chart templates (136–139)", description = "List, upsert, get, delete chart templates")
public class ChartTemplateController {

	private final ChartTemplateService chartTemplateService;

	/**
	 * Creates the controller.
	 *
	 * @param chartTemplateService chart template service
	 */
	public ChartTemplateController(ChartTemplateService chartTemplateService) {
		this.chartTemplateService = chartTemplateService;
	}

	/**
	 * Design doc 136 — lists chart templates for the current customer.
	 *
	 * @return chart template list DTO (name only, sorted by name)
	 */
	@GetMapping
	@Operation(summary = "136 Get chart template list")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Name-only list for this customer"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
	})
	public List<ChartTemplateListItemDto> list() {

		// Design doc 136.
		return chartTemplateService.list();
	}

	/**
	 * Design doc 137 — registers or updates a chart template.
	 *
	 * @param request name and content
	 * @return row update datetime (unix seconds)
	 */
	@PostMapping
	@Operation(summary = "137 Register or update chart template")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Upserted; body is { t } from updated_at"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "422", description = "Invalid name or content")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			content = @Content(
					examples = @ExampleObject(
							value = "{\"name\":\"My Dark\",\"content\":\"{\\\"theme\\\":\\\"dark\\\"}\"}")))
	public SystemDatetimeResponse upsert(@RequestBody(required = false) UpsertChartTemplateRequest request) {

		// Design doc 137.
		return chartTemplateService.upsert(request);
	}

	/**
	 * Design doc 138 — loads one chart template.
	 *
	 * @param name path template name (max 64)
	 * @return name and content
	 */
	@GetMapping("/{name}")
	@Operation(summary = "138 Get chart template")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "name and content"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Not found or other customer"),
			@ApiResponse(responseCode = "422", description = "Blank or name longer than 64")
	})
	public ChartTemplateDto get(
			@Parameter(description = "Template name (max 64)") @PathVariable("name") String name) {

		// Design doc 138.
		return chartTemplateService.get(name);
	}

	/**
	 * Design doc 139 — deletes one chart template.
	 *
	 * @param name path template name (max 64)
	 * @return system datetime (unix seconds)
	 */
	@DeleteMapping("/{name}")
	@Operation(summary = "139 Delete chart template")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Deleted; body is { t } unix seconds"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Not found or other customer"),
			@ApiResponse(responseCode = "422", description = "Blank or name longer than 64")
	})
	public SystemDatetimeResponse delete(
			@Parameter(description = "Template name (max 64)") @PathVariable("name") String name) {

		// Design doc 139.
		return chartTemplateService.delete(name);
	}
}
