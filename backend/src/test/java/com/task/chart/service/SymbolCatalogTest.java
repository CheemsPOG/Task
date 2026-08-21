/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.task.chart.config.AppProperties;
import com.task.chart.service.impl.CurrencyPairServiceImpl;
import com.task.chart.service.impl.SymbolCatalogImpl;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for demo FX symbol resolve.
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
 *   <tr><td>1.1.0</td><td>2026/08/20</td><td>Task</td><td>Expect CTFX/FOREX from app.tradingview</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
class SymbolCatalogTest {

	private final SymbolCatalog catalog = new SymbolCatalogImpl(new CurrencyPairServiceImpl(), new AppProperties());

	@Test
	void resolvesUsdJpyByDisplayTickerAndProviderName() {
		assertEquals("USD/JPY", catalog.find("USD/JPY").shortName());
		assertEquals("USD/JPY", catalog.find("USDJPY").shortName());
		assertEquals("USD/JPY", catalog.find("FX:USD/JPY").shortName());
		assertEquals(1, catalog.find("USD/JPY").curpairCd());
		assertEquals(1000, catalog.find("USD/JPY").priceScale());
		assertEquals("CTFX", catalog.find("USD/JPY").exchange());
		assertEquals("FOREX", catalog.find("USD/JPY").type());
	}

	@Test
	void listsAllDemoPairs() {
		assertEquals(5, catalog.getAll().size());
		assertNotNull(catalog.find("EUR/USD"));
		assertNotNull(catalog.find("GBPUSD"));
		assertNull(catalog.find("ETH/USDT"));
		assertNull(catalog.find("Binance:ETH/USDT"));
	}
}
