/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * Forming-bar snapshot for Redis {@code peach:forming:*} / {@code peach:bars}.
 *
 * <p>This is the still-open candle Java already upserted into the design doc 121 warehouse so
 * widget {@code subscribeBars} matches {@code GET /api/history} last bar. {@code time} is unix
 * milliseconds. {@code QuoteBus} builds it from ingest; Python only relays {@code /ws/stream}.
 * It is not a tick ({@link FxQuoteMessage}), not a REST history DTO, and not OHLC computed in
 * Python.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/26</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public record FormingBarMessage(
		String curpairCd,
		String curpairName,
		String resolution,
		long periodMs,
		long time,
		double bidOpen,
		double bidHigh,
		double bidLow,
		double bidClose,
		double askOpen,
		double askHigh,
		double askLow,
		double askClose,
		double volume) {
}
