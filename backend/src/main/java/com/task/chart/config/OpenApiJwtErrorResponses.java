/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.config;

import com.task.chart.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Common JWT error responses for Swagger (Baeldung {@code @ApiResponses}).
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
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
		@ApiResponse(
				responseCode = "401",
				description = "Missing or invalid JWT",
				content = @Content(
						mediaType = "application/json",
						schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(
				responseCode = "422",
				description = "Validation failed (CODE:30020)",
				content = @Content(
						mediaType = "application/json",
						schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(
				responseCode = "404",
				description = "Resource not found (CODE:30404)",
				content = @Content(
						mediaType = "application/json",
						schema = @Schema(implementation = ErrorResponse.class)))
})
public @interface OpenApiJwtErrorResponses {
}
