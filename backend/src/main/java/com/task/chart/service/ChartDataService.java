package com.task.chart.service;

import com.task.chart.dto.BarDto;
import com.task.chart.dto.DatafeedConfigResponse;
import com.task.chart.dto.DatafeedConfigResponse.SymbolTypeDto;
import com.task.chart.dto.HistoryResponse;
import com.task.chart.dto.SearchSymbolDto;
import com.task.chart.dto.SymbolInfoDto;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChartDataService {

	private final SymbolCatalog symbolCatalog;
	private final MockBarGenerator mockBarGenerator;
	private final MockFxQuoteService mockFxQuoteService;

	public ChartDataService(
			SymbolCatalog symbolCatalog,
			MockBarGenerator mockBarGenerator,
			MockFxQuoteService mockFxQuoteService) {
		this.symbolCatalog = symbolCatalog;
		this.mockBarGenerator = mockBarGenerator;
		this.mockFxQuoteService = mockFxQuoteService;
	}

	public DatafeedConfigResponse config() {
		return new DatafeedConfigResponse(
				true,
				false,
				false,
				false,
				true,
				ResolutionMapper.SUPPORTED_RESOLUTIONS,
				List.of(),
				List.of(new SymbolTypeDto("forex", "forex")));
	}

	public long serverTimeSeconds() {
		return Instant.now().getEpochSecond();
	}

	public List<SearchSymbolDto> search(String query, String exchange, String type, int limit) {
		String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		int max = limit <= 0 ? 50 : Math.min(limit, 200);

		return symbolCatalog.getAll().stream()
				.filter(symbol -> exchange == null || exchange.isBlank() || symbol.exchange().equals(exchange))
				.filter(symbol -> type == null || type.isBlank() || symbol.type().equals(type))
				.filter(symbol -> needle.isEmpty()
						|| symbol.ticker().toLowerCase(Locale.ROOT).contains(needle)
						|| symbol.shortName().toLowerCase(Locale.ROOT).contains(needle)
						|| symbol.providerSymbol().toLowerCase(Locale.ROOT).contains(needle))
				.limit(max)
				.map(symbol -> new SearchSymbolDto(
						symbol.shortName(),
						symbol.fullName(),
						symbol.ticker(),
						symbol.shortName(),
						symbol.exchange(),
						symbol.type()))
				.toList();
	}

	public SymbolInfoDto resolve(String symbolName) {
		CachedSymbol symbol = symbolCatalog.find(symbolName);
		if (symbol == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown_symbol");
		}

		return new SymbolInfoDto(
				symbol.ticker(),
				symbol.shortName(),
				symbol.shortName(),
				symbol.type(),
				symbol.exchange(),
				symbol.exchange(),
				"24x7",
				"Etc/UTC",
				1,
				symbol.priceScale(),
				"price",
				true,
				ResolutionMapper.SECONDS_MULTIPLIERS,
				true,
				ResolutionMapper.INTRADAY_MULTIPLIERS,
				true,
				ResolutionMapper.DAILY_MULTIPLIERS,
				true,
				ResolutionMapper.WEEKLY_MULTIPLIERS,
				ResolutionMapper.MONTHLY_MULTIPLIERS,
				"ohlcv",
				ResolutionMapper.SUPPORTED_RESOLUTIONS,
				"streaming",
				symbol.providerSymbol());
	}

	public HistoryResponse history(String symbolName, String resolution, Long to, Integer countBack) {
		return history(symbolName, resolution, to, countBack, PriceComponent.MID);
	}

	public HistoryResponse history(
			String symbolName,
			String resolution,
			Long to,
			Integer countBack,
			String price) {
		return history(symbolName, resolution, to, countBack, PriceComponent.from(price));
	}

	public HistoryResponse history(
			String symbolName,
			String resolution,
			Long to,
			Integer countBack,
			PriceComponent price) {
		CachedSymbol symbol = symbolCatalog.find(symbolName);
		if (symbol == null) {
			return HistoryResponse.error("unknown_symbol");
		}

		Long periodMs = ResolutionMapper.periodMillis(resolution);
		if (periodMs == null) {
			return HistoryResponse.error("Unsupported resolution: " + resolution);
		}

		PriceComponent component = price == null ? PriceComponent.MID : price;
		long toMs = (to == null ? Instant.now().getEpochSecond() : to) * 1000L;
		int needed = countBack == null || countBack <= 0 ? 300 : countBack;
		List<BarDto> bars = mockBarGenerator.generate(symbol, periodMs, toMs, needed, component);
		if (bars.isEmpty()) {
			return HistoryResponse.empty();
		}
		return HistoryResponse.ok(stitchCurrentBar(symbol, periodMs, bars, component));
	}

	public CachedSymbol findSymbol(String symbolName) {
		return symbolCatalog.find(symbolName);
	}

	public String providerSymbol(String symbolName) {
		CachedSymbol symbol = symbolCatalog.find(symbolName);
		return symbol == null ? null : symbol.providerSymbol();
	}

	private List<BarDto> stitchCurrentBar(
			CachedSymbol symbol,
			long periodMs,
			List<BarDto> bars,
			PriceComponent price) {
		BarDto last = bars.get(bars.size() - 1);
		long currentOpen = Math.floorDiv(Instant.now().toEpochMilli() - 1, periodMs) * periodMs;
		if (last.time() != currentOpen) {
			return bars;
		}
		double close = mockFxQuoteService.currentPrice(symbol.curpairCd(), price);
		BarDto stitched = new BarDto(
				last.time(),
				last.open(),
				Math.max(last.high(), close),
				Math.min(last.low(), close),
				close,
				last.volume());
		List<BarDto> copy = new ArrayList<>(bars);
		copy.set(copy.size() - 1, stitched);
		return copy;
	}
}
