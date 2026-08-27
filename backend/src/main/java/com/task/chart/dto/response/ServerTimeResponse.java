/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * Unix server time in seconds for GET /api/time.
 *
 * <p>Design doc 122 uses field {@code t}; the TradingView datafeed reads {@code serverTime}.
 * Both carry the same epoch-seconds value (no milliseconds). {@code ChartDataController.time}
 * returns this from {@code ChartDataServiceImpl.serverTimeSeconds}. It is not the
 * {@code { "t": ... }} body used by layout/template delete and upsert
 * ({@link SystemDatetimeResponse}).
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
 *   <tr><td>1.1.0</td><td>2026/08/20</td><td>Task</td><td>Add doc field t alongside serverTime</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public record ServerTimeResponse(long serverTime, long t) {

	/**
	 * Builds a response where {@code serverTime} and {@code t} share one unix-seconds value.
	 *
	 * @param unixSeconds epoch seconds without milliseconds
	 */
	public ServerTimeResponse(long unixSeconds) {
		this(unixSeconds, unixSeconds);
	}

}
