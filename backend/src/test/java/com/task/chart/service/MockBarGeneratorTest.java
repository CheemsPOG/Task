package com.task.chart.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.task.chart.dto.BarDto;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockBarGeneratorTest {

	private final MockBarGenerator generator = new MockBarGenerator();
	private final CachedSymbol usdJpy = new SymbolCatalog(new CurrencyPairService()).find("USD/JPY");

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
}
