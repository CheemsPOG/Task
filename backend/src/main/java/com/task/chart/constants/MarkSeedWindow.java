/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.constants;

/**
 * Deterministic seed window for {@code m_tv_mark} (V3) and {@code m_tv_timescale_mark} (V4).
 *
 * <p>Unix-second inclusive bounds used by Flyway seed SQL and by tests for docs 125 / 126 so
 * {@code GET /api/marks} and {@code GET /api/timescale_marks} return rows. Marks have no
 * {@code customer_no}. {@link com.task.chart.service.impl.ChartDataServiceImpl} does not read these
 * constants at runtime. This is NOT bar warehouse times, NOT Redis, and NOT the widget.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Shared with timescale mark seed</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public final class MarkSeedWindow {

	/** 2026-08-18 00:00:00 UTC — inclusive lower bound covering seed marks. */
	public static final long FROM = 1_787_011_200L;

	/** 2026-08-21 00:00:00 UTC — inclusive upper bound covering seed marks. */
	public static final long TO = 1_787_270_400L;

	private MarkSeedWindow() {
	}
}
