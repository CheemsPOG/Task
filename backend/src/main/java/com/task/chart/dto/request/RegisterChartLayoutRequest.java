/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body for POST /api/layouts (design doc 127).
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
public record RegisterChartLayoutRequest(
		@Schema(description = "Layout name (max 64)", example = "My layout") String name,
		@Schema(description = "Widget layout JSON", example = "{\"pane\":1}") String content,
		@Schema(description = "Currency pair CD", example = "USDJPY") String symbol,
		@Schema(description = "Chart type / resolution", example = "1D") String resolution) {
}
