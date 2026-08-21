/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.DatafeedConfigResponse;
import com.task.chart.dto.response.HistoryResponse;
import com.task.chart.dto.response.SearchSymbolDto;
import com.task.chart.dto.response.SymbolInfoDto;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.util.List;

/**
 * TradingView datafeed search, resolve, config, and history.
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
public interface ChartDataService {

	DatafeedConfigResponse config();

	long serverTimeSeconds();

	List<SearchSymbolDto> search(String query, String exchange, String type, int limit);

	SymbolInfoDto resolve(String symbolName);

	HistoryResponse history(String symbolName, String resolution, Long to, Integer countBack);

	HistoryResponse history(
			String symbolName,
			String resolution,
			Long to,
			Integer countBack,
			String price);

	HistoryResponse history(
			String symbolName,
			String resolution,
			Long from,
			Long to,
			Integer countBack,
			String price,
			String bidAsk);

	HistoryResponse history(
			String symbolName,
			String resolution,
			Long to,
			Integer countBack,
			PriceComponent price);

	CachedSymbol findSymbol(String symbolName);

	String providerSymbol(String symbolName);
}
