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
 * <p>Docs 120–126: config from {@code app.tradingview}; history from Redis
 * {@code peach:{cache_set_*}:{CD}} (last bar matches ingest); resolve/search from {@code m_ccypairs}
 * and {@code m_season}; marks from {@code m_tv_mark} / {@code m_tv_timescale_mark} (no tenant).
 * {@link com.task.chart.controller.ChartDataController} is the HTTP caller.
 *
 * <p><strong>NOT:</strong> not {@link MockBarGeneratorImpl} (boot seed); not
 * {@code TickIngestWorker} (writes the last bar); not the Python WS; not the widget
 * {@code datafeed.ts}. Unknown pair on {@code /symbols} is <strong>404</strong>; the same
 * unknown CD on {@code /history} is <strong>422</strong>. Do not harmonize — see
 * {@code SystemOverviewDesign121Test}. {@code price=mid} is a widget quirk that maps to
 * {@code bid_ask=MID} (second input path for the same semantic).
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 *   <tr><td>1.0.2</td><td>2026/08/31</td><td>Task</td><td>Review comments on 422/404/500 paths</td></tr>
 *   <tr><td>1.0.3</td><td>2026/08/31</td><td>Task</td><td>Method overview Javadocs on helpers</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.3
 */
@Service
public class ChartDataServiceImpl implements ChartDataService {

	private final SymbolCatalog symbolCatalog;
	private final ChartCacheStore chartCacheStore;
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
	 * @param appProperties tradingview flags
	 * @param ccypairRepository pair master
	 * @param seasonRepository season master
	 * @param tvMarkRepository mark master
	 * @param tvTimescaleMarkRepository timescale mark master
	 */
	public ChartDataServiceImpl(
			SymbolCatalog symbolCatalog,
			ChartCacheStore chartCacheStore,
			AppProperties appProperties,
			CcypairRepository ccypairRepository,
			SeasonRepository seasonRepository,
			TvMarkRepository tvMarkRepository,
			TvTimescaleMarkRepository tvTimescaleMarkRepository) {
		this.symbolCatalog = symbolCatalog;
		this.chartCacheStore = chartCacheStore;
		this.appProperties = appProperties;
		this.ccypairRepository = ccypairRepository;
		this.seasonRepository = seasonRepository;
		this.tvMarkRepository = tvMarkRepository;
		this.tvTimescaleMarkRepository = tvTimescaleMarkRepository;
	}

	/**
	 * Doc 120 {@code GET /api/config}. Copies {@code app.tradingview} flags into the UDF
	 * {@code onReady} payload (search, marks, resolutions, one exchange/type).
	 * {@code supports_group_request} is hardcoded {@code false} — this demo has no
	 * group-resolve API; flipping the flag would make the widget call an endpoint we do not have.
	 */
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

	/**
	 * Doc 122. Unix <em>seconds</em> (same unit as Peach {@code t[]}), not millis.
	 */
	@Override
	public long serverTimeSeconds() {
		return Instant.now().getEpochSecond();
	}

	/**
	 * Doc 124 {@code GET /api/search} over active {@code m_ccypairs}. Empty query lists
	 * pairs; otherwise match display ticker or CD without the slash. Query longer than
	 * 10 is 422. A wrong exchange/type returns {@code []} (200), not 422, so the widget
	 * treats it as "no hits" instead of a datafeed error.
	 */
	@Override
	public List<SearchSymbolDto> search(String query, String exchange, String type, Integer limit) {
		AppProperties.TradingView tradingView = appProperties.getTradingView();
		String needle = query == null ? "" : query.trim();
		if (needle.length() > 10) {
			throw new ValidationException();
		}

		int effectiveLimit = resolveSearchLimit(limit, tradingView);

		// Unknown exchange/type is empty, not 422 — widget search should not hard-fail.
		if (!matchesConfiguredFilter(exchange, tradingView.getExchanges())
				|| !matchesConfiguredFilter(type, tradingView.getSymbolsTypes())) {
			return List.of();
		}

		// Empty query lists pairs; otherwise match display or CD without the slash.
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

	/**
	 * Doc 125. Marks are global demo seeds — no {@code customer_no}. After validation, a
	 * pair with no rows returns {@code []} (200), not 404. Resolution {@code 10} is valid
	 * on history but 422 here ({@link ResolutionMapper#MARKS_RESOLUTIONS}).
	 */
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

	/**
	 * Doc 126. Same contract as {@link #marks}: global, 422 on bad fields, 200 empty if none.
	 */
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

	/**
	 * Marks/timescale: 422 on blank symbol, resolution not in marks list (so {@code 10}
	 * fails here), or inverted from/to. Does <em>not</em> check the pair exists.
	 */
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

	/**
	 * Maps a mark row to UDF JSON. Label font is hardcoded ({@code #ffffff}, size 14) —
	 * not stored on {@code m_tv_mark}.
	 */
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

	/**
	 * Maps a timescale-mark row. Tooltip is a one-element list because the widget
	 * expects an array even when the table has a single string.
	 */
	private static TimescaleMarkDto toTimescaleMarkDto(TvTimescaleMark mark) {
		return new TimescaleMarkDto(
				mark.getId(),
				mark.getTimescaleMarkAt(),
				mark.getColor(),
				mark.getLabel(),
				List.of(mark.getTooltip()),
				"#ffffff");
	}

	/**
	 * Caps search page size. Omitted {@code limit} uses {@code search-default-limit};
	 * below 1 or above max is 422, not a silent clamp.
	 */
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

	/**
	 * Blank exchange/type means "no filter" (match all). A non-blank value must equal
	 * {@code app.tradingview} config; mismatch is handled by the caller as empty hits.
	 */
	private static boolean matchesConfiguredFilter(String requested, String configured) {
		if (requested == null || requested.isBlank()) {
			return true;
		}

		return configured.equalsIgnoreCase(requested.trim());
	}

	/**
	 * Search hit: ticker is the 6-char CD; display is {@code USD/JPY}. Japanese name
	 * comes from {@code ccypair_jp}.
	 */
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

	/**
	 * Doc 123. Unknown active pair is <strong>404</strong>. {@link #history} uses 422 for
	 * the same miss — tested in {@code SystemOverviewDesign121Test}; do not harmonize.
	 */
	@Override
	public SymbolInfoDto resolve(String symbolName) {
		String ccypairCd = requireCcypairCd(symbolName);
		Optional<Ccypair> found = ccypairRepository.findByCcypairCdAndIsDeleted(ccypairCd, Ccypair.ACTIVE);
		Ccypair pair = found.orElse(null);
		if (pair == null) {

			// Resolve/search: unknown pair is 404. /history uses 422 for the same miss
			// (SystemOverviewDesign121Test). Do not "harmonize" those statuses.
			throw new ResourceNotFoundException();
		}

		return toSymbolInfo(pair, currentSession());
	}

	/**
	 * Resolve-path CD: blank or not length-6 after {@link #normalizeCcypairCd} is 422
	 * (malformed ticker). Unknown-but-well-formed CD is 404 in {@link #resolve}.
	 */
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

	/**
	 * Resolve/layouts: strip {@code FX:} then slashes so {@code FX:USD/JPY} → {@code USDJPY}.
	 * History uses {@link #normalizeSymbolCd} instead (letter-only; {@code FX:} becomes 422).
	 */
	private static String normalizeCcypairCd(String symbolName) {
		String upper = symbolName.trim().toUpperCase(Locale.ROOT);

		// Widget may send FX:USD/JPY; masters use USDJPY.
		if (upper.startsWith("FX:")) {
			upper = upper.substring(3);
		}

		return upper.replace("/", "");
	}

	/**
	 * Session string for doc 123. Empty {@code m_season} is 500: the demo seed covers
	 * 2020–2099, so a miss means seed/config is broken, not a client typo.
	 */
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

		// Doc 123 session string: summer vs winter from m_season, not the widget clock.
		if (seasonCd == Season.DAYLIGHT_SAVING) {
			return tradingView.getTimeSummer();
		}

		return tradingView.getTimeWinter();
	}

	/**
	 * Builds UDF {@code SymbolInfo}: session from {@link #currentSession},
	 * {@code pricescale} from {@code rate_unit}, multipliers from {@link ResolutionMapper}.
	 * {@code minmov} is always 1; {@code volume_precision} path is unused (plots are price).
	 */
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

	/**
	 * {@code USDJPY} → {@code USD/JPY} for widget display. Non-6-char values pass through.
	 */
	private static String displayTicker(String ccypairCd) {
		if (ccypairCd == null || ccypairCd.length() != 6) {
			return ccypairCd;
		}

		return ccypairCd.substring(0, 3) + "/" + ccypairCd.substring(3);
	}

	/**
	 * {@code pricescale = 10^rate_unit} (pip display). Not the live quote precision.
	 */
	private static int priceScale(int rateUnit) {
		return BigDecimal.TEN.pow(rateUnit).intValueExact();
	}

	/**
	 * Doc 121 {@code GET /api/history}. Reads Redis only — does not compute OHLC. Unknown CD
	 * is <strong>422</strong> (not 404). {@code bid_ask} is required; {@code price} is used
	 * only when {@code bid_ask} is blank (widget quirk this datafeed does not send).
	 * Missing both is 422, not a silent MID default.
	 *
	 * <p>History strips to letters via {@link #normalizeSymbolCd}, so {@code FX:USD/JPY}
	 * becomes {@code FXUSDJPY} (length 8 → 422). Resolve/layouts use
	 * {@link #normalizeCcypairCd} which strips {@code FX:} first. Do not swap the two.
	 */
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

			// Widget quirk: price=mid is a second input for the same BID/ASK/MID side.
			effectiveBidAsk = PriceComponent.from(price).name();
		}
		validateHistoryRequest(symbolName, resolution, from, to, effectiveBidAsk);
		PriceComponent component = PriceComponent.fromBidAsk(effectiveBidAsk);
		return history(symbolName, resolution, from, to, countBack, component);
	}

	/**
	 * Redis read + BID/ASK/MID projection. Does not generate bars. Clamps {@code to} to
	 * now so the widget cannot request the future. Empty range is Peach {@code no_data}
	 * with optional {@code nextTime}, not 404.
	 *
	 * <p>The {@code price == null ? MID} line is defensive only — the public method always
	 * passes a non-null enum after {@code fromBidAsk}. This app's datafeed always sends
	 * {@code bid_ask}; missing both {@code bid_ask} and {@code price} is 422, not MID.
	 */
	private HistoryResponse history(
			String symbolName,
			String resolution,
			Long from,
			Long to,
			Integer countBack,
			PriceComponent price) {
		CachedSymbol symbol = symbolCatalog.find(symbolName);
		if (symbol == null) {

			// History is stricter than /symbols: unknown CD is 422, not 404.
			throw new ValidationException();
		}

		CacheNamespace namespace = CacheNamespace.fromTvResolution(resolution);
		if (namespace == null) {
			throw new ValidationException();
		}

		PriceComponent component = price == null ? PriceComponent.MID : price;
		long nowSec = Instant.now().getEpochSecond();
		Long queryFrom = from;
		Long queryTo = to;
		if (queryTo != null) {
			queryTo = Math.min(queryTo, nowSec);
		}

		// Redis ZSET first (doc 121); last bar is the forming candle ingest just wrote.
		List<CachedChartBar> cached = chartCacheStore.query(
				namespace,
				symbol.providerSymbol(),
				queryFrom,
				queryTo);

		// UDF countBack: keep the newest N bars. Older warehouse rows stay in Redis.
		if (countBack != null && countBack > 0 && cached.size() > countBack) {
			cached = cached.subList(cached.size() - countBack, cached.size());
		}

		if (cached.isEmpty()) {

			// Peach no_data + nextTime: widget can jump to the previous stored bar.
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
		return HistoryResponse.ok(bars);
	}

	/**
	 * History 422 rules: symbol letters must be length 6, resolution in
	 * {@link ResolutionMapper#HISTORY_RESOLUTIONS} (includes {@code 10}), {@code bid_ask}
	 * required (BID/ASK/MID), {@code from}/{@code to} both present or both omitted.
	 * Does not look up the pair — unknown CD is 422 later via {@link SymbolCatalog#find}.
	 */
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

		// from and to are both required or both omitted (UDF countBack path).
		if ((from == null) != (to == null)) {
			throw new ValidationException();
		}
		if (from != null && to != null && to < from) {
			throw new ValidationException();
		}
	}

	/**
	 * History-only: letters only so {@code USD/JPY} → {@code USDJPY}. {@code FX:USD/JPY}
	 * becomes {@code FXUSDJPY} (8 chars → 422). Resolve uses {@link #normalizeCcypairCd}.
	 *
	 * @param symbolName raw symbol query
	 * @return uppercase CD letters only
	 */
	static String normalizeSymbolCd(String symbolName) {
		return symbolName.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
	}
}
