package com.task.chart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.task.chart.dto.CurrencyPairDto;
import com.task.chart.dto.FxQuoteMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockFxQuoteServiceTest {

	@Test
	void snapshotKeepsBidAskMidRelationship() {
		CurrencyPairService pairs = new CurrencyPairService();
		MockFxQuoteService service = new MockFxQuoteService(pairs);

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
		CurrencyPairDto pair = new CurrencyPairService().find(1);
		assertThat(pair).isNotNull();
		assertThat(pair.curpairName()).isEqualTo("USDJPY");
		assertThat(pair.curpairDisplay()).isEqualTo("USD/JPY");
	}

	@Test
	void currentMidMatchesSnapshot() {
		MockFxQuoteService service = new MockFxQuoteService(new CurrencyPairService());
		FxQuoteMessage usdJpy = service.snapshot().stream()
				.filter(quote -> "1".equals(quote.curpairCd()))
				.findFirst()
				.orElseThrow();
		assertThat(service.currentMid(1)).isEqualTo(usdJpy.mid());
	}
}
