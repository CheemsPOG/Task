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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TradingView datafeed HTTP API (design docs 120–126). HTTP only; no SQL.
 *
 * <p>{@code GET /api/history} (121) reads {@code ChartCacheStore} (Redis
 * {@code cache_set_*} / warehouse {@code t_chart_*}). Live last bars are written
 * only by {@code TickIngestWorker}; this controller does not compute OHLC.
 *
 * <p><strong>NOT:</strong> no SQL, no Redis, no inline validation beyond query-param
 * binding — any of those here is a layering violation. Unknown symbol on
 * {@code /history} is 422; on {@code /symbols} it is 404 (service, not this class).
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>OpenAPI operation docs</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Datafeed (120–126)", description = "TradingView UDF: config, history, time, symbols, search, marks")
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
	 * Liveness check. Extra vs design docs 120–139.
	 *
	 * @return health payload
	 */
	@GetMapping("/health")
	@SecurityRequirements
	@Operation(summary = "Health check (no token)")
	@ApiResponse(responseCode = "200", description = "Service is up")
	public HealthResponse health() {

		// Extra vs design docs 120–139.
		return new HealthResponse("ok", "chart-backend");
	}

	/**
	 * Design doc 120 — datafeed configuration for {@code onReady}.
	 *
	 * @return supported resolutions and flags
	 */
	@GetMapping("/config")
	@Operation(summary = "120 Get datafeed configuration")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Widget onReady payload"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
	})
	public DatafeedConfigResponse config() {

		// Design doc 120.
		return chartDataService.config();
	}

	/**
	 * Design doc 122 — current server time.
	 *
	 * @return unix time in seconds
	 */
	@GetMapping("/time")
	@Operation(summary = "122 Get server time")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Unix seconds"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
	})
	public ServerTimeResponse time() {

		// Design doc 122.
		return new ServerTimeResponse(chartDataService.serverTimeSeconds());
	}

	/**
	 * Design doc 124 — symbol search over {@code m_ccypairs}.
	 *
	 * @param query optional search text (max length 10)
	 * @param exchange optional exchange filter (widget)
	 * @param type optional type filter (widget)
	 * @param limit optional max hits; default/max from {@code app.tradingview}
	 * @return matching symbols
	 */
	@GetMapping("/search")
	@Operation(summary = "124 Get symbol list")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Matching pairs"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "422", description = "query too long or bad limit")
	})
	public List<SearchSymbolDto> search(
			@Parameter(description = "Partial CD or JP name, max 10") @RequestParam(required = false) String query,
			@Parameter(description = "Widget filter; CTFX or empty") @RequestParam(required = false) String exchange,
			@Parameter(description = "Widget filter; FOREX or empty") @RequestParam(required = false) String type,
			@Parameter(description = "1–100, default 100") @RequestParam(required = false) Integer limit) {

		// Design doc 124.
		return chartDataService.search(query, exchange, type, limit);
	}

	/**
	 * Design doc 123 — resolves one ticker to TradingView symbol info.
	 *
	 * @param symbol currency pair CD ({@code USDJPY}) or widget display name ({@code USD/JPY})
	 * @return symbol metadata
	 */
	@GetMapping("/symbols")
	@Operation(summary = "123 Get symbol information")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "LibrarySymbolInfo"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "404", description = "Unknown pair"),
			@ApiResponse(responseCode = "422", description = "symbol missing or not length 6")
	})
	public SymbolInfoDto symbols(
			@Parameter(description = "USDJPY or USD/JPY", example = "USDJPY")
			@RequestParam(required = false) String symbol) {

		// Design doc 123.
		return chartDataService.resolve(symbol);
	}

	/**
	 * Design doc 125 — chart marks for the visible range.
	 *
	 * @param symbol currency pair CD or display name
	 * @param resolution TradingView resolution
	 * @param from range start unix seconds (inclusive)
	 * @param to range end unix seconds (inclusive)
	 * @return mark list
	 */
	@GetMapping("/marks")
	@Operation(summary = "125 Get marks list")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Marks in range (may be empty)"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "422", description = "Missing params or bad resolution")
	})
	public List<MarkDto> marks(
			@Parameter(example = "USDJPY") @RequestParam(required = false) String symbol,
			@Parameter(example = "1D") @RequestParam(required = false) String resolution,
			@Parameter(example = "1787011200") @RequestParam(required = false) Long from,
			@Parameter(example = "1787270400") @RequestParam(required = false) Long to) {

		// Design doc 125.
		return chartDataService.marks(symbol, resolution, from, to);
	}

	/**
	 * Design doc 126 — timescale marks for the visible range.
	 *
	 * @param symbol currency pair CD or display name
	 * @param resolution TradingView resolution
	 * @param from range start unix seconds (inclusive)
	 * @param to range end unix seconds (inclusive)
	 * @return timescale mark list
	 */
	@GetMapping("/timescale_marks")
	@Operation(summary = "126 Get timescale marks list")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Timescale marks in range"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "422", description = "Missing params or bad resolution")
	})
	public List<TimescaleMarkDto> timescaleMarks(
			@Parameter(example = "USDJPY") @RequestParam(required = false) String symbol,
			@Parameter(example = "1D") @RequestParam(required = false) String resolution,
			@Parameter(example = "1787011200") @RequestParam(required = false) Long from,
			@Parameter(example = "1787270400") @RequestParam(required = false) Long to) {

		// Design doc 126.
		return chartDataService.timescaleMarks(symbol, resolution, from, to);
	}

	/**
	 * Design doc 121 — historical OHLC columnar arrays from {@code ChartCacheStore}
	 * (Redis {@code cache_set_*} / {@code t_chart_*}). Does not write bars.
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
	@Operation(summary = "121 Get bars")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "OHLC arrays (s=ok or no_data)"),
			@ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "422", description = "Unknown/invalid symbol, missing bid_ask, or bad range")
	})
	public HistoryResponse history(
			@Parameter(example = "USDJPY") @RequestParam(required = false) String symbol,
			@Parameter(example = "1D") @RequestParam(required = false) String resolution,
			@Parameter(description = "Unix seconds; required with to") @RequestParam(required = false) Long from,
			@Parameter(description = "Unix seconds; required with from") @RequestParam(required = false) Long to,
			@RequestParam(required = false) Integer countBack,
			@Parameter(description = "Widget alias: bid/ask/mid") @RequestParam(required = false) String price,
			@Parameter(description = "BID, MID, or ASK", example = "MID") @RequestParam(required = false)
					String bid_ask) {

		// Design doc 121: HTTP only; ChartDataService reads ChartCacheStore (Redis / t_chart_*).
		return chartDataService.history(symbol, resolution, from, to, countBack, price, bid_ask);
	}

}
