/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body for POST /api/layouts (design doc 127).
 *
 * <p>This JSON is the named chart layout the client wants to store: widget {@code content},
 * 6-char pair {@code symbol}, and {@code resolution} (stored as {@code chart_type}).
 * {@code ChartLayoutController} passes it to {@code ChartLayoutServiceImpl}, which inserts
 * {@code m_tv_chart_layout} for the JWT {@code customer_no}. It is not the PUT update path
 * (design doc 128) and not a chart-template or study-template body.
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public record RegisterChartLayoutRequest(
		@Schema(description = "Layout name (max 64)", example = "My layout") String name,
		@Schema(description = "Widget layout JSON", example = "{\"pane\":1}") String content,
		@Schema(description = "Currency pair CD", example = "USDJPY") String symbol,
		@Schema(description = "Chart type / resolution", example = "1D") String resolution) {
}
