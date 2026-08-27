/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Chart REST backend ({@code com.task.chart}).
 *
 * <p>Layering: {@code controller} (HTTP only, docs 120–139) → {@code service} /
 * {@code service.impl} → {@code repository} or {@code cache}. HTTP JSON uses
 * {@code dto.request} / {@code dto.response}. JWT lives in {@code security}.
 *
 * <p>Live pipeline: {@code TickIngestWorker} is the only OHLC writer.
 * {@code DemoTickEngine} is the mock LP (replace that class for a real feed).
 * {@code QuoteBus} publishes {@code peach:quotes} (ticks) and {@code peach:bars}
 * (forming candles). Python only relays those channels.
 * {@code GET /api/history} reads {@code ChartCacheStore} (Redis
 * {@code cache_set_*} / warehouse {@code t_chart_*}).
 *
 * <p>Folder map: {@code backend/src/main/java/README.md}.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/20</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.1.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
package com.task.chart;
