/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.task.chart.config.AppProperties;
import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.BarDto;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import com.task.chart.service.impl.CurrencyPairServiceImpl;
import com.task.chart.service.impl.MockBarGeneratorImpl;
import com.task.chart.service.impl.SymbolCatalogImpl;
import com.task.chart.util.DemoMarket;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for mock historical bars.
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
class MockBarGeneratorTest {

	private final MockBarGenerator generator = new MockBarGeneratorImpl();
	private final CachedSymbol usdJpy =
			new SymbolCatalogImpl(new CurrencyPairServiceImpl(), new AppProperties()).find("USD/JPY");

	@Test
	void historyIsDeterministicAndDropsBarAtTo() {
		long toMs = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
		long period = 86_400_000L;
		List<BarDto> first = generator.generate(usdJpy, period, toMs, 10);
		List<BarDto> second = generator.generate(usdJpy, period, toMs, 10);

		assertThat(first).hasSize(10);
		assertThat(second).isEqualTo(first);
		assertThat(first.get(first.size() - 1).time()).isLessThan(toMs);
		assertThat(first.get(0).time()).isEqualTo(first.get(1).time() - period);
		for (BarDto bar : first) {
			assertThat(bar.high()).isGreaterThanOrEqualTo(Math.max(bar.open(), bar.close()));
			assertThat(bar.low()).isLessThanOrEqualTo(Math.min(bar.open(), bar.close()));
			assertThat(bar.volume()).isPositive();
		}
	}

	@Test
	void bidAskMidBarsAreOffsetAndShapedDifferently() {
		long toMs = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
		long period = 86_400_000L;
		BarDto mid = generator.generate(usdJpy, period, toMs, 1, PriceComponent.MID).get(0);
		BarDto bid = generator.generate(usdJpy, period, toMs, 1, PriceComponent.BID).get(0);
		BarDto ask = generator.generate(usdJpy, period, toMs, 1, PriceComponent.ASK).get(0);

		assertThat(bid.close()).isLessThan(mid.close());
		assertThat(ask.close()).isGreaterThan(mid.close());
		assertThat(ask.open()).isCloseTo(bid.open() + DemoMarket.fullSpread("USDJPY"), within(0.001));
		assertThat(ask.close()).isCloseTo(bid.close() + DemoMarket.fullSpread("USDJPY"), within(0.001));
		assertThat(mid.close()).isCloseTo((bid.close() + ask.close()) / 2.0, within(0.001));
		assertThat(ask.high()).isGreaterThan(mid.high());
		assertThat(bid.low()).isLessThan(mid.low());
		assertThat(bid.high()).isNotEqualTo(ask.high());
		assertThat(bid.time()).isEqualTo(mid.time());
		assertThat(ask.time()).isEqualTo(mid.time());
	}
}
