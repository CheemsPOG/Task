package com.task.chart.service;

import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class SymbolCatalog {

	private static final long CACHE_TTL_SECONDS = 600;

	private final BinanceClient binanceClient;
	private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

	public SymbolCatalog(BinanceClient binanceClient) {
		this.binanceClient = binanceClient;
	}

	public List<CachedSymbol> getAll() {
		CacheEntry current = cache.get();
		if (current != null && Instant.now().isBefore(current.expiresAt())) {
			return current.symbols();
		}

		synchronized (this) {
			current = cache.get();
			if (current != null && Instant.now().isBefore(current.expiresAt())) {
				return current.symbols();
			}

			List<CachedSymbol> symbols = loadFromBinance();
			cache.set(new CacheEntry(symbols, Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
			return symbols;
		}
	}

	public CachedSymbol find(String symbolName) {
		if (symbolName == null || symbolName.isBlank()) {
			return null;
		}
		String needle = symbolName.trim().toLowerCase(Locale.ROOT);
		return getAll().stream()
				.filter(symbol -> matches(symbol, needle))
				.findFirst()
				.orElse(null);
	}

	private static boolean matches(CachedSymbol symbol, String needle) {
		return symbol.ticker().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.fullName().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.shortName().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.providerSymbol().toLowerCase(Locale.ROOT).equals(needle);
	}

	private List<CachedSymbol> loadFromBinance() {
		JsonNode data = binanceClient.exchangeInfo();
		JsonNode symbolsNode = data == null ? null : data.get("symbols");
		if (symbolsNode == null || !symbolsNode.isArray()) {
			return List.of();
		}

		List<CachedSymbol> symbols = new ArrayList<>();
		for (JsonNode symbol : symbolsNode) {
			if (!"TRADING".equals(symbol.path("status").asText())
					|| symbol.path("isSpotTradingAllowed").asBoolean(true) == false) {
				continue;
			}

			String base = symbol.path("baseAsset").asText();
			String quote = symbol.path("quoteAsset").asText();
			if (base.isBlank() || quote.isBlank()) {
				continue;
			}

			String shortName = base + "/" + quote;
			String ticker = ResolutionMapper.EXCHANGE + ":" + shortName;
			String tickSize = findTickSize(symbol.path("filters"));
			symbols.add(new CachedSymbol(
					shortName,
					ticker,
					ticker,
					symbol.path("symbol").asText(),
					ResolutionMapper.EXCHANGE,
					"crypto",
					tickSizeToPriceScale(tickSize)));
		}

		symbols.sort(Comparator.comparing(CachedSymbol::ticker));
		return List.copyOf(symbols);
	}

	private static String findTickSize(JsonNode filters) {
		if (filters == null || !filters.isArray()) {
			return null;
		}
		for (JsonNode filter : filters) {
			if ("PRICE_FILTER".equals(filter.path("filterType").asText())) {
				return filter.path("tickSize").asText(null);
			}
		}
		return null;
	}

	static int tickSizeToPriceScale(String tickSize) {
		if (tickSize == null || tickSize.isBlank()) {
			return 100;
		}
		String trimmed = tickSize.replaceAll("0+$", "");
		int dot = trimmed.indexOf('.');
		if (dot < 0) {
			return 1;
		}
		return (int) Math.pow(10, trimmed.length() - dot - 1);
	}

	public record CachedSymbol(
			String shortName,
			String fullName,
			String ticker,
			String providerSymbol,
			String exchange,
			String type,
			int priceScale) {
	}

	private record CacheEntry(List<CachedSymbol> symbols, Instant expiresAt) {
	}
}
