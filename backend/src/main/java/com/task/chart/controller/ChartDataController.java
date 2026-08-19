package com.task.chart.controller;

import com.task.chart.dto.DatafeedConfigResponse;
import com.task.chart.dto.HealthResponse;
import com.task.chart.dto.HistoryResponse;
import com.task.chart.dto.MarkDto;
import com.task.chart.dto.SearchSymbolDto;
import com.task.chart.dto.ServerTimeResponse;
import com.task.chart.dto.SymbolInfoDto;
import com.task.chart.dto.TimescaleMarkDto;
import com.task.chart.service.ChartDataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChartDataController {

	private final ChartDataService chartDataService;

	public ChartDataController(ChartDataService chartDataService) {
		this.chartDataService = chartDataService;
	}

	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse("ok", "chart-backend");
	}

	@GetMapping("/config")
	public DatafeedConfigResponse config() {
		return chartDataService.config();
	}

	@GetMapping("/time")
	public ServerTimeResponse time() {
		return new ServerTimeResponse(chartDataService.serverTimeSeconds());
	}

	@GetMapping("/search")
	public List<SearchSymbolDto> search(
			@RequestParam(required = false, defaultValue = "") String query,
			@RequestParam(required = false) String exchange,
			@RequestParam(required = false) String type,
			@RequestParam(required = false, defaultValue = "50") int limit) {
		return chartDataService.search(query, exchange, type, limit);
	}

	@GetMapping("/symbols")
	public SymbolInfoDto symbols(@RequestParam String symbol) {
		return chartDataService.resolve(symbol);
	}

	@GetMapping("/history")
	public HistoryResponse history(
			@RequestParam String symbol,
			@RequestParam String resolution,
			@RequestParam(required = false) Long from,
			@RequestParam(required = false) Long to,
			@RequestParam(required = false) Integer countBack) {
		return chartDataService.history(symbol, resolution, to, countBack);
	}

	@GetMapping("/marks")
	public List<MarkDto> marks(
			@RequestParam(required = false) String symbol,
			@RequestParam(required = false) Long from,
			@RequestParam(required = false) Long to,
			@RequestParam(required = false) String resolution) {
		return chartDataService.marks(from, to);
	}

	@GetMapping("/timescale-marks")
	public List<TimescaleMarkDto> timescaleMarks(
			@RequestParam(required = false) String symbol,
			@RequestParam(required = false) Long from,
			@RequestParam(required = false) Long to,
			@RequestParam(required = false) String resolution) {
		return chartDataService.timescaleMarks(from, to);
	}
}
