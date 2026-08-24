/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.cache.CacheNamespace;
import com.task.chart.cache.CachedChartBar;
import com.task.chart.cache.ChartCacheStore;
import com.task.chart.config.AppProperties;
import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.BarDto;
import com.task.chart.dto.response.DatafeedConfigResponse;
import com.task.chart.dto.response.DatafeedConfigResponse.ExchangeDto;
import com.task.chart.dto.response.DatafeedConfigResponse.SymbolTypeDto;
import com.task.chart.dto.response.HistoryResponse;
import com.task.chart.dto.response.MarkDto;
import com.task.chart.dto.response.SearchSymbolDto;
import com.task.chart.dto.response.SymbolInfoDto;
import com.task.chart.dto.response.TimescaleMarkDto;
import com.task.chart.entity.Ccypair;
import com.task.chart.entity.Season;
import com.task.chart.entity.TvMark;
import com.task.chart.entity.TvTimescaleMark;
import com.task.chart.exception.ResourceNotFoundException;
import com.task.chart.exception.ServerErrorException;
import com.task.chart.exception.ValidationException;
import com.task.chart.repository.CcypairRepository;
import com.task.chart.repository.SeasonRepository;
import com.task.chart.repository.TvMarkRepository;
import com.task.chart.repository.TvTimescaleMarkRepository;
import com.task.chart.service.ChartDataService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ChartDataService}.
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
@Service
public class ChartDataServiceImpl implements ChartDataService {

	private final SymbolCatalog symbolCatalog;
	private final ChartCacheStore chartCacheStore;
	private final MockFxQuoteService mockFxQuoteService;
	private final AppProperties appProperties;
	private final CcypairRepository ccypairRepository;
	private final SeasonRepository seasonRepository;
	private final TvMarkRepository tvMarkRepository;
	private final TvTimescaleMarkRepository tvTimescaleMarkRepository;

	/**
	 * Creates the datafeed service.
	 *
	 * @param symbolCatalog in-memory pair catalog
	 * @param chartCacheStore Redis bar cache
	 * @param mockFxQuoteService live mid/bid/ask for stitching
	 * @param appProperties tradingview flags
	 * @param ccypairRepository pair master
	 * @param seasonRepository season master
	 * @param tvMarkRepository mark master
	 * @param tvTimescaleMarkRepository timescale mark master
	 */
	public ChartDataServiceImpl(
			SymbolCatalog symbolCatalog,
			ChartCacheStore chartCacheStore,
			MockFxQuoteService mockFxQuoteService,
			AppProperties appProperties,
			CcypairRepository ccypairRepository,
			SeasonRepository seasonRepository,
			TvMarkRepository tvMarkRepository,
			TvTimescaleMarkRepository tvTimescaleMarkRepository) {
		this.symbolCatalog = symbolCatalog;
		this.chartCacheStore = chartCacheStore;
		this.mockFxQuoteService = mockFxQuoteService;
		this.appProperties = appProperties;
		this.ccypairRepository = ccypairRepository;
		this.seasonRepository = seasonRepository;
		this.tvMarkRepository = tvMarkRepository;
		this.tvTimescaleMarkRepository = tvTimescaleMarkRepository;
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
	public List<SearchSymbolDto> search(String query, String exchange, String type, Integer limit) {
		AppProperties.TradingView tradingView = appProperties.getTradingView();
		String needle = query == null ? "" : query.trim();
		if (needle.length() > 10) {
			throw new ValidationException();
		}

		int effectiveLimit = resolveSearchLimit(limit, tradingView);
		if (!matchesConfiguredFilter(exchange, tradingView.getExchanges())
				|| !matchesConfiguredFilter(type, tradingView.getSymbolsTypes())) {
			return List.of();
		}

		boolean queryEmpty = needle.isEmpty();
		String needleCd = needle.replace("/", "");
		List<Ccypair> pairs = ccypairRepository.searchActive(
				Ccypair.ACTIVE,
				queryEmpty,
				needle,
				needleCd,
				PageRequest.of(0, effectiveLimit));

		String exchangeValue = tradingView.getExchanges();
		String typeValue = tradingView.getSymbolsTypes();
		return pairs.stream()
				.map(pair -> toSearchSymbol(pair, exchangeValue, typeValue))
				.toList();
	}

	@Override
	public List<MarkDto> marks(String symbol, String resolution, Long from, Long to) {
		validateMarksRequest(symbol, resolution, from, to);
		String ccypairCd = normalizeCcypairCd(symbol);
		List<TvMark> marks = tvMarkRepository
				.findByCcypairCdAndResolutionAndMarkAtGreaterThanEqualAndMarkAtLessThanEqualOrderByMarkAtAsc(
						ccypairCd,
						resolution,
						from,
						to);
		return marks.stream().map(ChartDataServiceImpl::toMarkDto).toList();
	}

	@Override
	public List<TimescaleMarkDto> timescaleMarks(String symbol, String resolution, Long from, Long to) {
		validateMarksRequest(symbol, resolution, from, to);
		String ccypairCd = normalizeCcypairCd(symbol);
		List<TvTimescaleMark> marks = tvTimescaleMarkRepository
				.findByCcypairCdAndResolutionAndTimescaleMarkAtGreaterThanEqualAndTimescaleMarkAtLessThanEqualOrderByTimescaleMarkAtAsc(
						ccypairCd,
						resolution,
						from,
						to);
		return marks.stream().map(ChartDataServiceImpl::toTimescaleMarkDto).toList();
	}

	private static void validateMarksRequest(String symbol, String resolution, Long from, Long to) {
		if (symbol == null || symbol.isBlank()) {
			throw new ValidationException();
		}
		if (resolution == null || resolution.isBlank() || !ResolutionMapper.isMarksResolution(resolution)) {
			throw new ValidationException();
		}
		if (from == null || to == null) {
			throw new ValidationException();
		}
		if (to < from) {
			throw new ValidationException();
		}
	}

	private static MarkDto toMarkDto(TvMark mark) {
		return new MarkDto(
				mark.getId(),
				mark.getMarkAt(),
				mark.getColor(),
				mark.getMarkText(),
				mark.getLabel(),
				"#ffffff",
				14);
	}

	private static TimescaleMarkDto toTimescaleMarkDto(TvTimescaleMark mark) {
		return new TimescaleMarkDto(
				mark.getId(),
				mark.getTimescaleMarkAt(),
				mark.getColor(),
				mark.getLabel(),
				List.of(mark.getTooltip()),
				"#ffffff");
	}

	private static int resolveSearchLimit(Integer limit, AppProperties.TradingView tradingView) {
		int maxLimit = tradingView.getSearchMaxLimit();
		if (limit == null) {
			return tradingView.getSearchDefaultLimit();
		}
		if (limit < 1 || limit > maxLimit) {
			throw new ValidationException();
		}

		return limit;
	}

	private static boolean matchesConfiguredFilter(String requested, String configured) {
		if (requested == null || requested.isBlank()) {
			return true;
		}

		return configured.equalsIgnoreCase(requested.trim());
	}

	private static SearchSymbolDto toSearchSymbol(Ccypair pair, String exchange, String type) {
		String ccypairCd = pair.getCcypairCd();
		String display = displayTicker(ccypairCd);
		return new SearchSymbolDto(
				ccypairCd,
				display,
				display,
				pair.getCcypairJp(),
				exchange,
				type);
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

		String ccypairCd = normalizeCcypairCd(symbolName);
		if (ccypairCd.length() != 6) {
			throw new ValidationException();
		}

		return ccypairCd;
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
		String displayName = displayTicker(ccypairCd);
		return new SymbolInfoDto(
				displayName,
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
		return history(symbolName, resolution, to, countBack, "mid");
	}

	@Override
	public HistoryResponse history(
			String symbolName,
			String resolution,
			Long to,
			Integer countBack,
			String price) {
		String bidAsk = PriceComponent.from(price).name();
		Long effectiveTo = to == null ? Instant.now().getEpochSecond() : to;
		Long periodSec = ResolutionMapper.periodMillis(resolution);
		long stepSec = periodSec == null ? 60L : Math.max(1L, periodSec / 1000L);
		int needed = countBack == null || countBack <= 0 ? 300 : countBack;
		Long from = effectiveTo - (long) needed * stepSec;
		return history(symbolName, resolution, from, effectiveTo, countBack, price, bidAsk);
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
		String effectiveBidAsk = bidAsk;
		if ((effectiveBidAsk == null || effectiveBidAsk.isBlank())
				&& price != null
				&& !price.isBlank()) {
			effectiveBidAsk = PriceComponent.from(price).name();
		}
		validateHistoryRequest(symbolName, resolution, from, to, effectiveBidAsk);
		PriceComponent component = PriceComponent.fromBidAsk(effectiveBidAsk);
		return history(symbolName, resolution, from, to, countBack, component);
	}

	@Override
	public HistoryResponse history(
			String symbolName,
			String resolution,
			Long to,
			Integer countBack,
			PriceComponent price) {
		String bidAsk = (price == null ? PriceComponent.MID : price).name();
		return history(symbolName, resolution, to, countBack, bidAsk.toLowerCase(Locale.ROOT));
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

		CacheNamespace namespace = CacheNamespace.fromTvResolution(resolution);
		if (namespace == null) {
			return HistoryResponse.error("Unsupported resolution: " + resolution);
		}

		PriceComponent component = price == null ? PriceComponent.MID : price;
		long nowSec = Instant.now().getEpochSecond();
		Long queryFrom = from;
		Long queryTo = to;
		if (queryTo != null) {
			queryTo = Math.min(queryTo, nowSec);
		}

		List<CachedChartBar> cached = chartCacheStore.query(
				namespace,
				symbol.providerSymbol(),
				queryFrom,
				queryTo);

		if (countBack != null && countBack > 0 && cached.size() > countBack) {
			cached = cached.subList(cached.size() - countBack, cached.size());
		}

		if (cached.isEmpty()) {
			Long nextTimeSeconds = null;
			if (from != null) {
				nextTimeSeconds = chartCacheStore.nextTimeBefore(
						namespace,
						symbol.providerSymbol(),
						from);
			}
			return HistoryResponse.empty(nextTimeSeconds);
		}

		List<BarDto> bars = new ArrayList<>(cached.size());
		for (CachedChartBar row : cached) {
			bars.add(row.toBarDto(component));
		}
		return HistoryResponse.ok(stitchCurrentBar(symbol, namespace.periodMillis(), bars, component));
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
		String normalizedCd = normalizeSymbolCd(symbolName);
		if (normalizedCd.length() != 6) {
			throw new ValidationException();
		}
		if (resolution == null || resolution.isBlank() || !ResolutionMapper.isHistoryResolution(resolution)) {
			throw new ValidationException();
		}
		if (bidAsk == null || bidAsk.isBlank()) {
			throw new ValidationException();
		}
		try {
			PriceComponent.fromBidAsk(bidAsk);
		} catch (IllegalArgumentException ex) {
			throw new ValidationException();
		}
		if ((from == null) != (to == null)) {
			throw new ValidationException();
		}
		if (from != null && to != null && to < from) {
			throw new ValidationException();
		}
	}

	/**
	 * Strips non-letters so {@code USD/JPY} becomes {@code USDJPY} (length-6 CD).
	 *
	 * @param symbolName raw symbol query
	 * @return uppercase CD letters only
	 */
	static String normalizeSymbolCd(String symbolName) {
		return symbolName.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
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
