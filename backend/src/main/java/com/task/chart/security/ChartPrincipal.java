/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

/**
 * Authenticated chart user (username + tenant customer number).
 *
 * <p>Built from the access JWT {@code sub} and {@code customer_no} claim by {@link JwtService}.
 * {@link JwtAuthenticationFilter} places this on Spring {@code SecurityContext}; layout and template
 * services then filter rows by {@code customerNo} ({@code demo} is 1, {@code demo2} is 2). This is NOT
 * a Peach SSO principal, NOT {@link RefreshTokenSession} (that comes from Redis), and NOT the widget.
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
public record ChartPrincipal(String username, long customerNo) {
}
