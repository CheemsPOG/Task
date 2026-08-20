package com.task.chart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SymbolCatalogTest {

	private final SymbolCatalog catalog = new SymbolCatalog(new CurrencyPairService());

	@Test
	void resolvesUsdJpyByDisplayTickerAndProviderName() {
		assertEquals("USD/JPY", catalog.find("USD/JPY").shortName());
		assertEquals("USD/JPY", catalog.find("USDJPY").shortName());
		assertEquals("USD/JPY", catalog.find("FX:USD/JPY").shortName());
		assertEquals(1, catalog.find("USD/JPY").curpairCd());
		assertEquals(1000, catalog.find("USD/JPY").priceScale());
		assertEquals("", catalog.find("USD/JPY").exchange());
		assertEquals("forex", catalog.find("USD/JPY").type());
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
