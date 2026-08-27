/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.request.UpsertIndicatorTemplateRequest;
import com.task.chart.dto.response.IndicatorTemplateDto;
import com.task.chart.dto.response.IndicatorTemplateListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import com.task.chart.service.IndicatorTemplateService;
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
 * Indicator template HTTP API (design docs 132–135). HTTP only; no SQL.
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Add 133 upsert, 134 get, 135 delete</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
 */
@RestController
@RequestMapping("/api/indicator-templates")
@Tag(name = "Indicator templates (132–135)", description = "List, upsert, get, delete study templates")
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
	 * Design doc 132 — lists indicator templates for the current customer.
	 *
	 * @return indicator template list DTO (name only, sorted by name)
	 */
	@GetMapping
	@Operation(summary = "132 Get indicator template list")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Name-only list for this customer"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
	})
	public List<IndicatorTemplateListItemDto> list() {

		// Design doc 132.
		return indicatorTemplateService.list();
	}

	/**
	 * Design doc 133 — registers or updates an indicator template.
	 *
	 * @param request name and content
	 * @return row update datetime (unix seconds)
	 */
	@PostMapping
	@Operation(summary = "133 Register or update indicator template")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Upserted; body is { t } from updated_at"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "422", description = "Invalid name or content")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			content = @Content(
					examples = @ExampleObject(
							value = "{\"name\":\"My RSI\",\"content\":\"{\\\"studies\\\":[]}\"}")))
	public SystemDatetimeResponse upsert(@RequestBody(required = false) UpsertIndicatorTemplateRequest request) {

		// Design doc 133.
		return indicatorTemplateService.upsert(request);
	}

	/**
	 * Design doc 134 — loads one indicator template.
	 *
	 * @param name path template name (max 64)
	 * @return name and content
	 */
	@GetMapping("/{name}")
	@Operation(summary = "134 Get indicator template")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "name and content"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Not found or other customer"),
			@ApiResponse(responseCode = "422", description = "Blank or name longer than 64")
	})
	public IndicatorTemplateDto get(
			@Parameter(description = "Template name (max 64)") @PathVariable("name") String name) {

		// Design doc 134.
		return indicatorTemplateService.get(name);
	}

	/**
	 * Design doc 135 — deletes one indicator template.
	 *
	 * @param name path template name (max 64)
	 * @return system datetime (unix seconds)
	 */
	@DeleteMapping("/{name}")
	@Operation(summary = "135 Delete indicator template")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Deleted; body is { t } unix seconds"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Not found or other customer"),
			@ApiResponse(responseCode = "422", description = "Blank or name longer than 64")
	})
	public SystemDatetimeResponse delete(
			@Parameter(description = "Template name (max 64)") @PathVariable("name") String name) {

		// Design doc 135.
		return indicatorTemplateService.delete(name);
	}
}
