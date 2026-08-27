/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * Successful refresh payload with a new Bearer access token.
 *
 * <p>Extra versus design docs 120–139. {@code AuthController} returns this from
 * {@code POST /api/auth/refresh} after rotating the HttpOnly refresh cookie.
 * {@code expiresIn} is seconds for the new access JWT. It is not {@link LoginResponse}
 * (no {@code refreshExpiresIn}) and does not put the refresh token in JSON.
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
public record RefreshResponse(String accessToken, String tokenType, long expiresIn) {
}
