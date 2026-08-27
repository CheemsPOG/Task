/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

/**
 * User identity stored against an opaque refresh token in Redis.
 *
 * <p>Value parsed from {@code peach:auth:refresh:{uuid}} ({@code username|customerNo}) by
 * {@link RefreshTokenStore}. Used only to mint a new access JWT on {@code POST /api/auth/refresh}.
 * This is NOT {@link ChartPrincipal} (that comes from the access JWT), NOT Peach S-01, and NOT
 * written to the browser as JSON.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/25</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public record RefreshTokenSession(String username, long customerNo) {
}
