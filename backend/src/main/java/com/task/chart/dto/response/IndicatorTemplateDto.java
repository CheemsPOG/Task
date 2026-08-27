/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * GET /api/indicator-templates/{name} payload (design doc 134).
 *
 * <p>This JSON is one study (indicator) template ({@code name} + {@code content}) for the JWT
 * customer. {@code IndicatorTemplateServiceImpl} loads {@code m_tv_indicator_template} by unique
 * {@code (customer_no, name)}. It is not the name-only list row
 * ({@link IndicatorTemplateListItemDto}) and not a chart template (docs 136–139).
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
public record IndicatorTemplateDto(String name, String content) {
}
