/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.config.AppProperties;
import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.service.CurrencyPairService;
import com.task.chart.service.SymbolCatalog;
import com.task.chart.util.DemoMarket;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link SymbolCatalog}.
 *
 * <p>Builds an in-memory list of TradingView tickers from {@code GET /curpairs} ({@code m_ccypairs})
 * plus {@code app.tradingview} exchange/type. Doc 121 history and {@link ChartDataServiceImpl} resolve
 * {@code USDJPY} / {@code USD/JPY} / {@code FX:USD/JPY} here.
 *
 * <p><strong>NOT:</strong> not {@code GET /api/symbols} HTTP itself; not the Python WS; not the
 * widget. History ({@link ChartDataServiceImpl#history}) returns 422 for an unknown CD;
 * {@code GET /api/symbols} returns 404 — do not merge those paths.
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
 *   <tr><td>1.1.0</td><td>2026/08/20</td><td>Task</td><td>Align exchange/type with app.tradingview</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 *   <tr><td>1.1.2</td><td>2026/08/31</td><td>Task</td><td>Review comments: find returns null</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.2
 */
@Service
public class SymbolCatalogImpl implements SymbolCatalog {

	private final List<CachedSymbol> symbols;

	/**
	 * Builds the in-memory symbol list from {@code /curpairs} and {@code app.tradingview}.
	 *
	 * @param currencyPairService demo FX catalog
	 * @param appProperties exchange / type flags
	 */
	public SymbolCatalogImpl(CurrencyPairService currencyPairService, AppProperties appProperties) {
		AppProperties.TradingView tradingView = appProperties.getTradingView();
		String exchange = tradingView.getExchanges();
		String type = tradingView.getSymbolsTypes();

		// Built once at startup from m_ccypairs. Pair catalog edits need a restart.
		this.symbols = currencyPairService.list().stream()
				.map(pair -> fromPair(pair, exchange, type))
				.toList();
	}

	/**
	 * Snapshot of the in-memory catalog. Same list as construction time — not refreshed
	 * until process restart.
	 */
	@Override
	public List<CachedSymbol> getAll() {
		return symbols;
	}

	/**
	 * Returns {@code null} on miss — does <em>not</em> throw. {@link ChartDataServiceImpl}
	 * turns that into 422 on history and 404 on {@code /symbols}. Keep the throw at the
	 * caller so those statuses stay different.
	 */
	@Override
	public CachedSymbol find(String symbolName) {
		if (symbolName == null || symbolName.isBlank()) {
			return null;
		}

		String needle = symbolName.trim().toLowerCase(Locale.ROOT);
		return symbols.stream()
				.filter(symbol -> matches(symbol, needle))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Catalog row for history lookup. {@code providerSymbol} is {@code USDJPY};
	 * {@code curpairCd} is numeric {@code priority} (Python quote id). Price scale
	 * comes from {@link DemoMarket}, not {@code m_ccypairs.rate_unit}.
	 */
	private static CachedSymbol fromPair(CurrencyPairDto pair, String exchange, String type) {
		String shortName = pair.curpairDisplay();
		return new CachedSymbol(
				shortName,
				shortName,
				shortName,
				pair.curpairName(),
				exchange,
				type,
				DemoMarket.priceScale(pair.curpairName()),
				pair.curpairCd());
	}

	/**
	 * Accepts {@code USD/JPY}, {@code USDJPY}, numeric {@code curpairCd}, and {@code fx:USD/JPY}.
	 * History validation still letter-strips {@code FX:} to {@code FXUSDJPY} (422) before
	 * this matcher runs — do not assume this method makes {@code FX:} legal on /history.
	 */
	private static boolean matches(CachedSymbol symbol, String needle) {
		return symbol.ticker().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.fullName().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.shortName().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.providerSymbol().toLowerCase(Locale.ROOT).equals(needle)
				|| String.valueOf(symbol.curpairCd()).equals(needle)
				|| ("fx:" + symbol.shortName()).toLowerCase(Locale.ROOT).equals(needle);
	}
}
