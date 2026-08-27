/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.cache.CachedChartBar;
import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.BarDto;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.util.List;

/**
 * Deterministic mock OHLCV generator; also fills Peach in-memory caches (Phase 1).
 *
 * <p>Boot-seed factory only: {@link com.task.chart.cache.ChartCacheWriter} writes past candles into
 * {@code t_chart_*} and Redis {@code cache_set_*}. Live prices come from
 * {@link com.task.chart.cache.TickIngestWorker}, not this interface. Implemented by
 * {@link com.task.chart.service.impl.MockBarGeneratorImpl}. This is NOT runtime ingest, NOT the
 * Python WS, and NOT the widget.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>peachBarAt for cache writer</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public interface MockBarGenerator {

	/**
	 * Builds {@code countBack} MID bars ending before {@code toMs}.
	 *
	 * @param symbol catalog symbol
	 * @param periodMs bar width
	 * @param toMs exclusive end (milliseconds)
	 * @param countBack number of bars
	 * @return oldest-first bars
	 */
	List<BarDto> generate(CachedSymbol symbol, long periodMs, long toMs, int countBack);

	/**
	 * Builds {@code countBack} bars on the requested price side.
	 *
	 * @param symbol catalog symbol
	 * @param periodMs bar width
	 * @param toMs exclusive end (milliseconds)
	 * @param countBack number of bars
	 * @param price BID, ASK, or MID
	 * @return oldest-first bars
	 */
	List<BarDto> generate(
			CachedSymbol symbol,
			long periodMs,
			long toMs,
			int countBack,
			PriceComponent price);

	/**
	 * One MID bar at the given open time.
	 *
	 * @param symbol catalog symbol
	 * @param periodMs bar width
	 * @param time bar open (milliseconds)
	 * @return mock bar
	 */
	BarDto barAt(CachedSymbol symbol, long periodMs, long time);

	/**
	 * One bar at the given open time on the requested price side.
	 *
	 * @param symbol catalog symbol
	 * @param periodMs bar width
	 * @param time bar open (milliseconds)
	 * @param price BID, ASK, or MID
	 * @return mock bar
	 */
	BarDto barAt(CachedSymbol symbol, long periodMs, long time, PriceComponent price);

	/**
	 * Builds one cache row with bid and ask OHLC at the bar open time.
	 *
	 * @param symbol catalog symbol
	 * @param periodMs bar period
	 * @param timeMs bar open (milliseconds)
	 * @return cached Peach bar
	 */
	CachedChartBar peachBarAt(CachedSymbol symbol, long periodMs, long timeMs);
}
