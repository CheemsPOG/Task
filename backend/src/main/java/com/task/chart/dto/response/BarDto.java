/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * One OHLCV bar used internally when mapping cache rows to GET /api/history.
 *
 * <p>{@code ChartDataServiceImpl} maps a Redis {@code CachedChartBar} BID/ASK/MID into this shape,
 * then {@link HistoryResponse#ok} converts it to Peach columnar arrays. {@code time} is unix
 * milliseconds at the candle open. It is not serialized on the history JSON (design doc 121).
 * It is not the warehouse row (that type keeps separate {@code bid_*} and {@code ask_*} columns)
 * and not the live forming-bar WebSocket payload.
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 *   <tr><td>1.1.0</td><td>2026/08/27</td><td>Task</td><td>Internal mapping only (doc 121 columnar JSON)</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
public record BarDto(
		long time,
		double open,
		double high,
		double low,
		double close,
		double volume) {
}
