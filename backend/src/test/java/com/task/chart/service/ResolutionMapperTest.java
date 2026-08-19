package com.task.chart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ResolutionMapperTest {

	@Test
	void mapsNativeBinanceIntervals() {
		assertEquals("1s", ResolutionMapper.toBinanceInterval("1S"));
		assertEquals("1m", ResolutionMapper.toBinanceInterval("1"));
		assertEquals("1h", ResolutionMapper.toBinanceInterval("60"));
		assertEquals("1d", ResolutionMapper.toBinanceInterval("1D"));
		assertEquals("1M", ResolutionMapper.toBinanceInterval("1M"));
	}

	@Test
	void returnsNullForLibraryAggregatedResolutions() {
		assertNull(ResolutionMapper.toBinanceInterval("2"));
		assertNull(ResolutionMapper.toBinanceInterval("10"));
		assertNull(ResolutionMapper.toBinanceInterval("90"));
	}
}
