/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * Chart layout id returned by register/update (design docs 127 / 128).
 *
 * <p>POST {@code /api/layouts} and PUT {@code /api/layouts/{id}} return only this {@code id} so the
 * client can fetch or update later. {@code ChartLayoutServiceImpl} builds it after insert/update of
 * {@code m_tv_chart_layout}. It is not the full layout ({@link ChartLayoutDto}) and not the
 * Peach-style datetime body used on delete ({@link SystemDatetimeResponse}).
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
public record ChartLayoutIdResponse(long id) {
}
