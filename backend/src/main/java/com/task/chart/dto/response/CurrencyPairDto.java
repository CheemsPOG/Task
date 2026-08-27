/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

/**
 * One currency pair from GET /curpairs ({@code curpairCd} = {@code m_ccypairs.priority}).
 *
 * <p>This catalog is extra versus design docs 120–139 so the quote toolbar can map live ticks.
 * {@code CurrencyPairServiceImpl} builds it from {@code m_ccypairs}; {@code TickIngestWorker},
 * {@code DemoTickEngine}, and {@code SymbolCatalogImpl} also consume it. {@code curpairCd} is the
 * integer {@code priority} (JSON number here; a string on the WebSocket tick). It is not the
 * 6-char {@code ccypair_cd} used by docs 123 / 124.
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Document curpairCd = priority</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public record CurrencyPairDto(int curpairCd, String curpairName, String curpairDisplay) {
}
