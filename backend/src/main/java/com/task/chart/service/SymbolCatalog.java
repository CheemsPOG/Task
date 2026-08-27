/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import java.util.List;

/**
 * Resolves chart tickers from the FX pair catalog.
 *
 * <p>In-memory TradingView symbols built once from {@code m_ccypairs} via {@link CurrencyPairService}.
 * Doc 121 history and ingest look up {@code providerSymbol} (e.g. {@code USDJPY}) here.
 * Implemented by {@link com.task.chart.service.impl.SymbolCatalogImpl}. This is NOT
 * {@code GET /api/symbols} itself (that is {@link ChartDataService#resolve}), NOT the Python WS, and
 * NOT the widget.
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
public interface SymbolCatalog {

	/**
	 * @return all seeded FX symbols
	 */
	List<CachedSymbol> getAll();

	/**
	 * Matches ticker, slash display, {@code FX:} prefix, pair CD, or numeric {@code curpairCd}.
	 *
	 * @param symbolName widget or query string
	 * @return catalog row, or {@code null}
	 */
	CachedSymbol find(String symbolName);

	/**
	 * TradingView in-memory symbol (ticker, Peach CD, scale, {@code curpairCd}).
	 */
	record CachedSymbol(
			String shortName,
			String fullName,
			String ticker,
			String providerSymbol,
			String exchange,
			String type,
			int priceScale,
			int curpairCd) {
	}
}
