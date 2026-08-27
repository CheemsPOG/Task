/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body for POST /api/chart-templates (design doc 137 upsert).
 *
 * <p>This JSON names a TradingView chart-style preset (theme/colors) and its {@code content} blob.
 * {@code ChartTemplateController} → {@code ChartTemplateServiceImpl} upserts
 * {@code m_tv_chart_templates} unique on {@code (customer_no, name)}. Insert sets all columns;
 * update changes {@code content} only. It is not a saved chart layout (docs 127–131) and not an
 * indicator/study template (docs 132–135).
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public record UpsertChartTemplateRequest(
		@Schema(description = "Template name (max 64)", example = "My Dark") String name,
		@Schema(description = "Chart template JSON", example = "{\"theme\":\"dark\"}") String content) {
}
