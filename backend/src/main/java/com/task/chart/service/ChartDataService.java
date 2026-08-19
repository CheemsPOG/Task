package com.task.chart.service;

import tools.jackson.databind.JsonNode;
import com.task.chart.dto.BarDto;
import com.task.chart.dto.DatafeedConfigResponse;
import com.task.chart.dto.DatafeedConfigResponse.ExchangeDto;
import com.task.chart.dto.DatafeedConfigResponse.SymbolTypeDto;
import com.task.chart.dto.HistoryResponse;
import com.task.chart.dto.MarkDto;
import com.task.chart.dto.SearchSymbolDto;
import com.task.chart.dto.SymbolInfoDto;
import com.task.chart.dto.TimescaleMarkDto;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChartDataService {

	private static final int KLINE_LIMIT = 1000;
	private static final int MAX_KLINE_REQUESTS = 25;
	private static final long ONE_DAY_SEC = 86_400L;

	private final BinanceClient binanceClient;
	private final SymbolCatalog symbolCatalog;

	public ChartDataService(BinanceClient binanceClient, SymbolCatalog symbolCatalog) {
		this.binanceClient = binanceClient;
		this.symbolCatalog = symbolCatalog;
	}

	public DatafeedConfigResponse config() {
		return new DatafeedConfigResponse(
				true,
				false,
				true,
				true,
				true,
				ResolutionMapper.SUPPORTED_RESOLUTIONS,
				List.of(new ExchangeDto(
						ResolutionMapper.EXCHANGE,
						ResolutionMapper.EXCHANGE,
						"Binance spot market")),
				List.of(new SymbolTypeDto("crypto", "crypto")));
	}

	public long serverTimeSeconds() {
		try {
			return Math.floorDiv(binanceClient.serverTimeMillis(), 1000);
		} catch (Exception ex) {
			return Instant.now().getEpochSecond();
		}
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
		SymbolCatalog.CachedSymbol symbol = symbolCatalog.find(symbolName);
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
		SymbolCatalog.CachedSymbol symbol = symbolCatalog.find(symbolName);
		if (symbol == null) {
			return HistoryResponse.error("unknown_symbol");
		}

		String interval = ResolutionMapper.toBinanceInterval(resolution);
		if (interval == null) {
			return HistoryResponse.error("Unsupported resolution: " + resolution);
		}

		long toMs = (to == null ? Instant.now().getEpochSecond() : to) * 1000L;
		int needed = countBack == null || countBack <= 0 ? 300 : countBack;

		try {
			List<JsonNode> raw = fetchKlinesBefore(symbol.providerSymbol(), interval, toMs, needed);
			List<BarDto> bars = new ArrayList<>();
			for (JsonNode entry : raw) {
				BarDto bar = toBar(entry);
				if (bar.time() < toMs) {
					bars.add(bar);
				}
			}

			if (bars.isEmpty()) {
				return HistoryResponse.empty();
			}
			return HistoryResponse.ok(bars);
		} catch (Exception ex) {
			return HistoryResponse.error(ex.getMessage());
		}
	}

	public List<MarkDto> marks(Long from, Long to) {
		long now = to == null ? Instant.now().getEpochSecond() : to;
		return List.of(
				new MarkDto("1", now, "red", "N", "#ffffff", 14, List.of("Latest bar mark")),
				new MarkDto("2", now - ONE_DAY_SEC, "green", "S", "#ffffff", 12, List.of("Signal from backend")),
				new MarkDto("3", now - ONE_DAY_SEC * 3, "blue", "T", "#ffffff", 12, List.of("Timescale sample")));
	}

	public List<TimescaleMarkDto> timescaleMarks(Long from, Long to) {
		long now = to == null ? Instant.now().getEpochSecond() : to;
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy").withZone(ZoneOffset.UTC);
		List<TimescaleMarkDto> marks = new ArrayList<>();
		for (int i = 1; i <= 8; i++) {
			long time = now - ONE_DAY_SEC * i;
			marks.add(new TimescaleMarkDto(
					"tsm" + i,
					time,
					i % 2 == 0 ? "#FFAA00" : "#089981",
					i == 1 ? "A" : "B",
					"#FFFFFF",
					List.of(fmt.format(Instant.ofEpochSecond(time)), "Backend timescale mark")));
		}
		return marks;
	}

	public String providerSymbol(String symbolName) {
		SymbolCatalog.CachedSymbol symbol = symbolCatalog.find(symbolName);
		return symbol == null ? null : symbol.providerSymbol();
	}

	private List<JsonNode> fetchKlinesBefore(String providerSymbol, String interval, long toMs, int countBack) {
		List<JsonNode> collected = new ArrayList<>();
		long endTime = toMs - 1;
		int requestCount = 0;

		while (collected.size() < countBack && requestCount < MAX_KLINE_REQUESTS) {
			int limit = Math.min(KLINE_LIMIT, countBack - collected.size());
			JsonNode batch = binanceClient.klines(providerSymbol, interval, endTime, limit);
			if (batch == null || !batch.isArray() || batch.isEmpty()) {
				break;
			}

			List<JsonNode> page = new ArrayList<>();
			batch.forEach(page::add);
			collected.addAll(0, page);
			requestCount += 1;
			endTime = page.get(0).get(0).asLong() - 1;

			if (page.size() < limit) {
				break;
			}
		}

		return collected;
	}

	private static BarDto toBar(JsonNode entry) {
		return new BarDto(
				entry.get(0).asLong(),
				entry.get(1).asDouble(),
				entry.get(2).asDouble(),
				entry.get(3).asDouble(),
				entry.get(4).asDouble(),
				entry.get(5).asDouble());
	}
}
