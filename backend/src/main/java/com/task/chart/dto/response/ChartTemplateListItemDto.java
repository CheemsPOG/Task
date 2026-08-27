/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * One row in GET /api/chart-templates list (design doc 136).
 *
 * <p>This is a name-only list entry (no {@code content}) for the JWT customer's chart templates.
 * {@code ChartTemplateServiceImpl} maps {@code m_tv_chart_templates} ordered by {@code name}
 * ascending. It is not the GET-by-name payload ({@link ChartTemplateDto}) and not an indicator
 * template list row.
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
public record ChartTemplateListItemDto(String name) {
}
