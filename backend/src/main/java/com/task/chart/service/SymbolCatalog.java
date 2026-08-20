package com.task.chart.service;

import com.task.chart.dto.CurrencyPairDto;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SymbolCatalog {

	private final List<CachedSymbol> symbols;

	public SymbolCatalog(CurrencyPairService currencyPairService) {
		this.symbols = currencyPairService.list().stream()
				.map(SymbolCatalog::fromPair)
				.toList();
	}

	public List<CachedSymbol> getAll() {
		return symbols;
	}

	public CachedSymbol find(String symbolName) {
		if (symbolName == null || symbolName.isBlank()) {
			return null;
		}
		String needle = symbolName.trim().toLowerCase(Locale.ROOT);
		return symbols.stream()
				.filter(symbol -> matches(symbol, needle))
				.findFirst()
				.orElse(null);
	}

	private static CachedSymbol fromPair(CurrencyPairDto pair) {
		String shortName = pair.curpairDisplay();
		return new CachedSymbol(
				shortName,
				shortName,
				shortName,
				pair.curpairName(),
				"",
				"forex",
				DemoMarket.priceScale(pair.curpairName()),
				pair.curpairCd());
	}

	private static boolean matches(CachedSymbol symbol, String needle) {
		return symbol.ticker().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.fullName().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.shortName().toLowerCase(Locale.ROOT).equals(needle)
				|| symbol.providerSymbol().toLowerCase(Locale.ROOT).equals(needle)
				|| String.valueOf(symbol.curpairCd()).equals(needle)
				|| ("fx:" + symbol.shortName()).toLowerCase(Locale.ROOT).equals(needle);
	}

	public record CachedSymbol(
			String shortName,
			String fullName,
			String ticker,
			String providerSymbol,
			String exchange,
			String type,
			int priceScale,
			int curpairCd) {
	}
}
