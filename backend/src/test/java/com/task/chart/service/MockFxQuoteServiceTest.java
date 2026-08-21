/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.dto.response.FxQuoteMessage;
import com.task.chart.service.impl.CurrencyPairServiceImpl;
import com.task.chart.service.impl.MockFxQuoteServiceImpl;
import com.task.chart.util.DemoMarket;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for mock FX quote relationships.
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
class MockFxQuoteServiceTest {

	@Test
	void snapshotKeepsBidAskMidRelationship() {
		CurrencyPairService pairs = new CurrencyPairServiceImpl();
		MockFxQuoteService service = new MockFxQuoteServiceImpl(pairs);

		for (int i = 0; i < 40; i++) {
			service.tick();
		}

		List<FxQuoteMessage> quotes = service.snapshot();
		assertThat(quotes).hasSize(5);
		for (FxQuoteMessage quote : quotes) {
			CurrencyPairDto pair = pairs.find(Integer.parseInt(quote.curpairCd()));
			assertThat(pair).isNotNull();
			assertThat(quote.bid()).isLessThan(quote.ask());
			assertThat(quote.ask()).isCloseTo(
					quote.bid() + DemoMarket.fullSpread(pair.curpairName()),
					within(0.001));
			assertThat(quote.mid()).isCloseTo((quote.bid() + quote.ask()) / 2.0, within(0.0000001));
			assertThat(quote.high()).isGreaterThanOrEqualTo(quote.ask());
			assertThat(quote.low()).isLessThanOrEqualTo(quote.bid());
			assertThat(quote.curpairCd()).isNotBlank();
			assertThat(quote.rateMiliSecondUTC()).isPositive();
		}
	}

	@Test
	void firstPairCodeResolvesToUsdJpy() {
		CurrencyPairDto pair = new CurrencyPairServiceImpl().find(1);
		assertThat(pair).isNotNull();
		assertThat(pair.curpairName()).isEqualTo("USDJPY");
		assertThat(pair.curpairDisplay()).isEqualTo("USD/JPY");
	}

	@Test
	void currentMidMatchesSnapshot() {
		MockFxQuoteService service = new MockFxQuoteServiceImpl(new CurrencyPairServiceImpl());
		FxQuoteMessage usdJpy = service.snapshot().stream()
				.filter(quote -> "1".equals(quote.curpairCd()))
				.findFirst()
				.orElseThrow();
		assertThat(service.currentMid(1)).isEqualTo(usdJpy.mid());
	}
}
