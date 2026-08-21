/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.response.DatafeedConfigResponse;
import com.task.chart.dto.response.HealthResponse;
import com.task.chart.dto.response.HistoryResponse;
import com.task.chart.dto.response.MarkDto;
import com.task.chart.dto.response.SearchSymbolDto;
import com.task.chart.dto.response.ServerTimeResponse;
import com.task.chart.dto.response.SymbolInfoDto;
import com.task.chart.dto.response.TimescaleMarkDto;
import com.task.chart.service.ChartDataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the TradingView Advanced Charts datafeed.
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
@RestController
@RequestMapping("/api")
public class ChartDataController {

	private final ChartDataService chartDataService;

	/**
	 * Creates the controller.
	 *
	 * @param chartDataService datafeed service
	 */
	public ChartDataController(ChartDataService chartDataService) {
		this.chartDataService = chartDataService;
	}

	/**
	 * Liveness check.
	 *
	 * @return health payload
	 */
	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse("ok", "chart-backend");
	}

	/**
	 * Datafeed configuration for {@code onReady}.
	 *
	 * @return supported resolutions and flags
	 */
	@GetMapping("/config")
	public DatafeedConfigResponse config() {
		return chartDataService.config();
	}

	/**
	 * Current server time.
	 *
	 * @return unix time in seconds
	 */
	@GetMapping("/time")
	public ServerTimeResponse time() {
		return new ServerTimeResponse(chartDataService.serverTimeSeconds());
	}

	/**
	 * Symbol search over {@code m_ccypairs} (design doc 124).
	 *
	 * @param query optional search text (max length 10)
	 * @param exchange optional exchange filter (widget)
	 * @param type optional type filter (widget)
	 * @param limit optional max hits; default/max from {@code app.tradingview}
	 * @return matching symbols
	 */
	@GetMapping("/search")
	public List<SearchSymbolDto> search(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) String exchange,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) Integer limit) {
		return chartDataService.search(query, exchange, type, limit);
	}

	/**
	 * Resolves one ticker to TradingView symbol info.
	 *
	 * @param symbol currency pair CD ({@code USDJPY}) or widget display name ({@code USD/JPY})
	 * @return symbol metadata
	 */
	@GetMapping("/symbols")
	public SymbolInfoDto symbols(@RequestParam(required = false) String symbol) {
		return chartDataService.resolve(symbol);
	}

	/**
	 * Chart marks for the visible range (design doc 125).
	 *
	 * @param symbol currency pair CD or display name
	 * @param resolution TradingView resolution
	 * @param from range start unix seconds (inclusive)
	 * @param to range end unix seconds (inclusive)
	 * @return mark list
	 */
	@GetMapping("/marks")
	public List<MarkDto> marks(
			@RequestParam(required = false) String symbol,
			@RequestParam(required = false) String resolution,
			@RequestParam(required = false) Long from,
			@RequestParam(required = false) Long to) {
		return chartDataService.marks(symbol, resolution, from, to);
	}

	/**
	 * Timescale marks for the visible range (design doc 126).
	 *
	 * @param symbol currency pair CD or display name
	 * @param resolution TradingView resolution
	 * @param from range start unix seconds (inclusive)
	 * @param to range end unix seconds (inclusive)
	 * @return timescale mark list
	 */
	@GetMapping("/timescale_marks")
	public List<TimescaleMarkDto> timescaleMarks(
			@RequestParam(required = false) String symbol,
			@RequestParam(required = false) String resolution,
			@RequestParam(required = false) Long from,
			@RequestParam(required = false) Long to) {
		return chartDataService.timescaleMarks(symbol, resolution, from, to);
	}

	/**
	 * Historical OHLCV bars.
	 *
	 * @param symbol ticker
	 * @param resolution TradingView resolution
	 * @param from optional start unix seconds; must be paired with {@code to}
	 * @param to end time in unix seconds
	 * @param countBack number of bars to return
	 * @param price {@code bid}, {@code ask}, or {@code mid} (widget)
	 * @param bidAsk {@code BID}, {@code ASK}, or {@code MID} (design doc 121)
	 * @return history payload
	 */
	@GetMapping("/history")
	public HistoryResponse history(
			@RequestParam(required = false) String symbol,
			@RequestParam(required = false) String resolution,
			@RequestParam(required = false) Long from,
			@RequestParam(required = false) Long to,
			@RequestParam(required = false) Integer countBack,
			@RequestParam(required = false) String price,
			@RequestParam(required = false) String bid_ask) {
		return chartDataService.history(symbol, resolution, from, to, countBack, price, bid_ask);
	}

}
