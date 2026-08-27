/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * System datetime payload (unix seconds) for delete-style Peach responses.
 *
 * <p>Layout delete (doc 131), indicator upsert/delete (docs 133 / 135), and chart-template
 * upsert/delete (docs 137 / 139) return {@code { "t": ... }}. Services build it from
 * {@code updated_at} (upsert) or {@code Instant.now()} (delete). It is not
 * {@code GET /api/time} ({@link ServerTimeResponse}, which also has {@code serverTime}).
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
public record SystemDatetimeResponse(long t) {
}
