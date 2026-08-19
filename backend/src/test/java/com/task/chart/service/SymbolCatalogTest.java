package com.task.chart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SymbolCatalogTest {

	@Test
	void convertsTickSizeToPriceScale() {
		assertEquals(100, SymbolCatalog.tickSizeToPriceScale(null));
		assertEquals(1, SymbolCatalog.tickSizeToPriceScale("1.00000000"));
		assertEquals(100, SymbolCatalog.tickSizeToPriceScale("0.01000000"));
		assertEquals(100000000, SymbolCatalog.tickSizeToPriceScale("0.00000001"));
	}
}
