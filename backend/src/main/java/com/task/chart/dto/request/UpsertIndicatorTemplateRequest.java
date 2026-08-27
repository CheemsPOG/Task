/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body for POST /api/indicator-templates (design doc 133 upsert).
 *
 * <p>This JSON names a TradingView study (indicator) template and its {@code content} blob.
 * {@code IndicatorTemplateController} → {@code IndicatorTemplateServiceImpl} upserts
 * {@code m_tv_indicator_template} unique on {@code (customer_no, name)}. Insert sets all columns;
 * update changes {@code content} only. It is not a chart layout (docs 127–131) and not a chart
 * template (docs 136–139).
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
public record UpsertIndicatorTemplateRequest(
		@Schema(description = "Template name (max 64)", example = "My RSI") String name,
		@Schema(description = "Study template JSON", example = "{\"studies\":[]}") String content) {
}
