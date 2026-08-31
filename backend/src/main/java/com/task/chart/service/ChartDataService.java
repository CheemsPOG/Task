/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.dto.response.DatafeedConfigResponse;
import com.task.chart.dto.response.HistoryResponse;
import com.task.chart.dto.response.MarkDto;
import com.task.chart.dto.response.SearchSymbolDto;
import com.task.chart.dto.response.SymbolInfoDto;
import com.task.chart.dto.response.TimescaleMarkDto;
import java.util.List;

/**
 * TradingView datafeed search, resolve, config, and history.
 *
 * <p>Business rules for docs 120–126: config from {@code app.tradingview}, history from Redis
 * {@code peach:{cache_set_*}:*} (not live ingest), symbols/search from {@code m_ccypairs} /
 * {@code m_season}, marks from {@code m_tv_mark} / {@code m_tv_timescale_mark}.
 * {@link com.task.chart.controller.ChartDataController} is the only HTTP caller. Implemented by
 * {@link com.task.chart.service.impl.ChartDataServiceImpl}. This is NOT {@link MockBarGenerator}
 * (boot seed), NOT {@code TickIngestWorker} (live SSOT), NOT the Python WS, and NOT the widget
 * {@code datafeed.ts}.
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
public interface ChartDataService {

	/**
	 * Doc 120 {@code GET /api/config} from {@code app.tradingview}.
	 *
	 * @return UDF onReady flags
	 */
	DatafeedConfigResponse config();

	/**
	 * Doc 122 {@code GET /api/time}.
	 *
	 * @return unix seconds
	 */
	long serverTimeSeconds();

	/**
	 * Doc 124 {@code GET /api/search} over active {@code m_ccypairs}.
	 *
	 * @param query ticker substring (max 10)
	 * @param exchange optional exchange filter
	 * @param type optional type filter
	 * @param limit page size
	 * @return matching symbols
	 */
	List<SearchSymbolDto> search(String query, String exchange, String type, Integer limit);

	/**
	 * Doc 125 {@code GET /api/marks} from {@code m_tv_mark} (global, no tenant).
	 *
	 * @param symbol pair CD
	 * @param resolution TV resolution
	 * @param from unix seconds inclusive
	 * @param to unix seconds inclusive
	 * @return marks in range
	 */
	List<MarkDto> marks(String symbol, String resolution, Long from, Long to);

	/**
	 * Doc 126 {@code GET /api/timescale_marks} from {@code m_tv_timescale_mark}.
	 *
	 * @param symbol pair CD
	 * @param resolution TV resolution
	 * @param from unix seconds inclusive
	 * @param to unix seconds inclusive
	 * @return timescale marks in range
	 */
	List<TimescaleMarkDto> timescaleMarks(String symbol, String resolution, Long from, Long to);

	/**
	 * Doc 123 {@code GET /api/symbols}.
	 *
	 * @param symbolName ticker or CD
	 * @return UDF symbol info
	 */
	SymbolInfoDto resolve(String symbolName);

	/**
	 * Doc 121 history from Redis / warehouse.
	 *
	 * @param symbolName ticker
	 * @param resolution TV resolution
	 * @param from unix seconds
	 * @param to unix seconds
	 * @param countBack optional trim
	 * @param price fallback side when {@code bidAsk} is blank ({@code bid}/{@code ask}/{@code mid})
	 * @param bidAsk design-doc {@code BID} / {@code ASK} / {@code MID}
	 * @return UDF history
	 */
	HistoryResponse history(
			String symbolName,
			String resolution,
			Long from,
			Long to,
			Integer countBack,
			String price,
			String bidAsk);
}
