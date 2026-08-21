/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.task.chart.util.ResolutionMapper;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for resolution to period mapping.
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
class ResolutionMapperTest {

	@Test
	void mapsNativePeriodMillis() {
		assertEquals(1_000L, ResolutionMapper.periodMillis("1S"));
		assertEquals(60_000L, ResolutionMapper.periodMillis("1"));
		assertEquals(600_000L, ResolutionMapper.periodMillis("10"));
		assertEquals(3_600_000L, ResolutionMapper.periodMillis("60"));
		assertEquals(86_400_000L, ResolutionMapper.periodMillis("1D"));
		assertEquals(2_592_000_000L, ResolutionMapper.periodMillis("1M"));
	}

	@Test
	void returnsNullForLibraryAggregatedResolutions() {
		assertNull(ResolutionMapper.periodMillis("2"));
		assertNull(ResolutionMapper.periodMillis("90"));
	}

	@Test
	void marksResolutionsExcludeTen() {
		org.junit.jupiter.api.Assertions.assertTrue(ResolutionMapper.isMarksResolution("1D"));
		org.junit.jupiter.api.Assertions.assertTrue(ResolutionMapper.isHistoryResolution("10"));
		org.junit.jupiter.api.Assertions.assertFalse(ResolutionMapper.isMarksResolution("10"));
	}

	@Test
	void mapsTradingViewResolutionToPeachChartType() {
		assertEquals("1S", ResolutionMapper.toPeachChartType("1S"));
		assertEquals("1M", ResolutionMapper.toPeachChartType("1"));
		assertEquals("10M", ResolutionMapper.toPeachChartType("10"));
		assertEquals("DAY", ResolutionMapper.toPeachChartType("1D"));
		assertEquals("WEEK", ResolutionMapper.toPeachChartType("1W"));
		assertEquals("MONTH", ResolutionMapper.toPeachChartType("1M"));
		assertNull(ResolutionMapper.toPeachChartType("2"));
	}
}
