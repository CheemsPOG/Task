package com.task.chart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ResolutionMapperTest {

	@Test
	void mapsNativePeriodMillis() {
		assertEquals(1_000L, ResolutionMapper.periodMillis("1S"));
		assertEquals(60_000L, ResolutionMapper.periodMillis("1"));
		assertEquals(3_600_000L, ResolutionMapper.periodMillis("60"));
		assertEquals(86_400_000L, ResolutionMapper.periodMillis("1D"));
		assertEquals(2_592_000_000L, ResolutionMapper.periodMillis("1M"));
	}

	@Test
	void returnsNullForLibraryAggregatedResolutions() {
		assertNull(ResolutionMapper.periodMillis("2"));
		assertNull(ResolutionMapper.periodMillis("10"));
		assertNull(ResolutionMapper.periodMillis("90"));
	}
}
