/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * One mock FX quote tick payload.
 *
 * <p>This JSON is the live BID/ASK/MID tick on Redis {@code peach:quote:*} / {@code peach:quotes}.
 * {@code DemoTickEngine} builds it; {@code TickIngestWorker} and {@code QuoteBus} publish it;
 * Python relays {@code /ws/fx-quotes}. {@code curpairCd} is the string form of
 * {@code m_ccypairs.priority}. Extra versus docs 120–139. It is not a forming candle
 * ({@link FormingBarMessage}) and not REST {@link CurrencyPairDto} (integer {@code curpairCd}).
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public record FxQuoteMessage(
		String curpairCd,
		long rateMiliSecondUTC,
		double bid,
		double ask,
		double mid,
		double high,
		double low) {
}
