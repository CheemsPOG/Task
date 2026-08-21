/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.config.AppProperties;
import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.BarDto;
import com.task.chart.dto.response.DatafeedConfigResponse;
import com.task.chart.dto.response.DatafeedConfigResponse.ExchangeDto;
import com.task.chart.dto.response.DatafeedConfigResponse.SymbolTypeDto;
import com.task.chart.dto.response.HistoryResponse;
import com.task.chart.dto.response.SearchSymbolDto;
import com.task.chart.dto.response.SymbolInfoDto;
import com.task.chart.entity.Ccypair;
import com.task.chart.entity.Season;
import com.task.chart.exception.ResourceNotFoundException;
import com.task.chart.exception.ServerErrorException;
import com.task.chart.exception.ValidationException;
import com.task.chart.repository.CcypairRepository;
import com.task.chart.repository.SeasonRepository;
import com.task.chart.service.ChartDataService;
import com.task.chart.service.MockBarGenerator;
import com.task.chart.service.MockFxQuoteService;
import com.task.chart.service.SymbolCatalog;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import com.task.chart.util.ResolutionMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ChartDataService}.
 */
@Service
public class ChartDataServiceImpl implements ChartDataService {

	private final SymbolCatalog symbolCatalog;
	private final MockBarGenerator mockBarGenerator;
	private final MockFxQuoteService mockFxQuoteService;
	private final AppProperties appProperties;
	private final CcypairRepository ccypairRepository;
	private final SeasonRepository seasonRepository;

	public ChartDataServiceImpl(
			SymbolCatalog symbolCatalog,
			MockBarGenerator mockBarGenerator,
			MockFxQuoteService mockFxQuoteService,
			AppProperties appProperties,
			CcypairRepository ccypairRepository,
			SeasonRepository seasonRepository) {
		this.symbolCatalog = symbolCatalog;
		this.mockBarGenerator = mockBarGenerator;
		this.mockFxQuoteService = mockFxQuoteService;
		this.appProperties = appProperties;
		this.ccypairRepository = ccypairRepository;
		this.seasonRepository = seasonRepository;
	}

	@Override
	public DatafeedConfigResponse config() {
		AppProperties.TradingView tradingView = appProperties.getTradingView();
		String exchange = tradingView.getExchanges();
		String symbolType = tradingView.getSymbolsTypes();
		return new DatafeedConfigResponse(
				tradingView.isSupportsSearch(),
				false,
				tradingView.isSupportsMarks(),
				tradingView.isSupportsTimescaleMarks(),
				tradingView.isSupportsTime(),
				List.copyOf(tradingView.getSupportedResolutions()),
				List.of(new ExchangeDto(exchange, exchange, exchange)),
				List.of(new SymbolTypeDto(symbolType, symbolType)));
	}

	@Override
	public long serverTimeSeconds() {
		return Instant.now().getEpochSecond();
	}

	@Override
	public List<SearchSymbolDto> search(String query, String exchange, String type, int limit) {
		String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		int max = limit <= 0 ? 50 : Math.min(limit, 200);

		return symbolCatalog.getAll().stream()
				.filter(symbol -> exchange == null || exchange.isBlank()
						|| symbol.exchange().equalsIgnoreCase(exchange))
				.filter(symbol -> type == null || type.isBlank()
						|| symbol.type().equalsIgnoreCase(type))
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

	@Override
	public SymbolInfoDto resolve(String symbolName) {
		String ccypairCd = requireCcypairCd(symbolName);
		Optional<Ccypair> found = ccypairRepository.findByCcypairCdAndIsDeleted(ccypairCd, Ccypair.ACTIVE);
		Ccypair pair = found.orElse(null);
		if (pair == null) {
			throw new ResourceNotFoundException();
		}

		return toSymbolInfo(pair, currentSession());
	}

	private static String requireCcypairCd(String symbolName) {
		if (symbolName == null || symbolName.isBlank()) {
			throw new ValidationException();
		}

		return normalizeCcypairCd(symbolName);
	}

	private static String normalizeCcypairCd(String symbolName) {
		String upper = symbolName.trim().toUpperCase(Locale.ROOT);
		if (upper.startsWith("FX:")) {
			upper = upper.substring(3);
		}

		return upper.replace("/", "");
	}

	private String currentSession() {
		Instant now = Instant.now();
		List<Integer> seasonCds = List.of(Season.DAYLIGHT_SAVING, Season.STANDARD);
		List<Season> seasons = seasonRepository
				.findBySeasonCdInAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtDesc(
						seasonCds,
						now,
						now);
		if (seasons.isEmpty()) {
			throw new ServerErrorException();
		}

		Season season = seasons.get(0);
		int seasonCd = season.getSeasonCd();
		AppProperties.TradingView tradingView = appProperties.getTradingView();
		if (seasonCd == Season.DAYLIGHT_SAVING) {
			return tradingView.getTimeSummer();
		}

		return tradingView.getTimeWinter();
	}

	private SymbolInfoDto toSymbolInfo(Ccypair pair, String session) {
		AppProperties.TradingView tradingView = appProperties.getTradingView();
		String ccypairCd = pair.getCcypairCd();
		return new SymbolInfoDto(
				displayTicker(ccypairCd),
				ccypairCd,
				pair.getCcypairJp(),
				tradingView.getSymbolsTypes(),
				tradingView.getExchanges(),
				tradingView.getExchanges(),
				session,
				tradingView.getTimezone(),
				1,
				priceScale(pair.getRateUnit()),
				"price",
				tradingView.isHasSeconds(),
				ResolutionMapper.SECONDS_MULTIPLIERS,
				tradingView.isHasIntraday(),
				List.copyOf(tradingView.getIntradayMultipliers()),
				true,
				ResolutionMapper.DAILY_MULTIPLIERS,
				true,
				ResolutionMapper.WEEKLY_MULTIPLIERS,
				ResolutionMapper.MONTHLY_MULTIPLIERS,
				tradingView.getVisiblePlotsSet(),
				List.copyOf(tradingView.getSupportedResolutions()),
				"streaming",
				ccypairCd);
	}

	private static String displayTicker(String ccypairCd) {
		if (ccypairCd == null || ccypairCd.length() != 6) {
			return ccypairCd;
		}

		return ccypairCd.substring(0, 3) + "/" + ccypairCd.substring(3);
	}

	private static int priceScale(int rateUnit) {
		return BigDecimal.TEN.pow(rateUnit).intValueExact();
	}

	@Override
	public HistoryResponse history(String symbolName, String resolution, Long to, Integer countBack) {
		return history(symbolName, resolution, null, to, countBack, null, null);
	}

	@Override
	public HistoryResponse history(
			String symbolName,
			String resolution,
			Long to,
			Integer countBack,
			String price) {
		return history(symbolName, resolution, null, to, countBack, price, null);
	}

	@Override
	public HistoryResponse history(
			String symbolName,
			String resolution,
			Long from,
			Long to,
			Integer countBack,
			String price,
			String bidAsk) {
		validateHistoryRequest(symbolName, resolution, from, to, bidAsk);
		PriceComponent component = resolvePriceComponent(price, bidAsk);
		return history(symbolName, resolution, from, to, countBack, component);
	}

	@Override
	public HistoryResponse history(
			String symbolName,
			String resolution,
			Long to,
			Integer countBack,
			PriceComponent price) {
		return history(symbolName, resolution, null, to, countBack, price);
	}

	private HistoryResponse history(
			String symbolName,
			String resolution,
			Long from,
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
		if (from != null && to != null && (countBack == null || countBack <= 0)) {
			needed = Math.max(1, (int) ((toMs - from * 1000L) / periodMs) + 2);
		}
		List<BarDto> bars = mockBarGenerator.generate(symbol, periodMs, toMs, needed, component);
		if (from != null) {
			long fromMs = from * 1000L;
			bars = bars.stream().filter(bar -> bar.time() >= fromMs).toList();
		}
		if (bars.isEmpty()) {
			return HistoryResponse.empty();
		}
		return HistoryResponse.ok(stitchCurrentBar(symbol, periodMs, bars, component));
	}

	private static void validateHistoryRequest(
			String symbolName,
			String resolution,
			Long from,
			Long to,
			String bidAsk) {
		if (symbolName == null || symbolName.isBlank()) {
			throw new ValidationException();
		}
		if (resolution == null || resolution.isBlank() || !ResolutionMapper.isHistoryResolution(resolution)) {
			throw new ValidationException();
		}
		if (from != null && to == null) {
			throw new ValidationException();
		}
		if (from != null && to != null && to < from) {
			throw new ValidationException();
		}
		if (bidAsk != null && !bidAsk.isBlank()) {
			try {
				PriceComponent.fromBidAsk(bidAsk);
			} catch (IllegalArgumentException ex) {
				throw new ValidationException();
			}
		}
	}

	private static PriceComponent resolvePriceComponent(String price, String bidAsk) {
		if (bidAsk != null && !bidAsk.isBlank()) {
			return PriceComponent.fromBidAsk(bidAsk);
		}
		return PriceComponent.from(price);
	}

	@Override
	public CachedSymbol findSymbol(String symbolName) {
		return symbolCatalog.find(symbolName);
	}

	@Override
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
