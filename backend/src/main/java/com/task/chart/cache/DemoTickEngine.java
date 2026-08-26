/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.dto.response.FxQuoteMessage;
import com.task.chart.service.CurrencyPairService;
import com.task.chart.util.DemoMarket;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Demo Peach-feed stand-in: Gaussian BID walk, ASK = BID + spread, MID = (BID + ASK) / 2.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/26</td><td>Task</td><td>新規作成</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@Component
public class DemoTickEngine {

	private final CurrencyPairService currencyPairService;
	private final ChartCacheStore chartCacheStore;
	private final Map<Integer, SimulatedQuote> quotes = new ConcurrentHashMap<>();

	/**
	 * Creates the engine.
	 *
	 * @param currencyPairService pair catalog
	 * @param chartCacheStore last 1S bar for a smooth handoff from seed
	 */
	public DemoTickEngine(CurrencyPairService currencyPairService, ChartCacheStore chartCacheStore) {
		this.currencyPairService = currencyPairService;
		this.chartCacheStore = chartCacheStore;
	}

	/**
	 * Seeds in-memory quotes from the last 1S warehouse bar (or {@link DemoMarket} if empty).
	 */
	public void loadFromWarehouse() {
		quotes.clear();
		for (CurrencyPairDto pair : currencyPairService.list()) {
			quotes.put(pair.curpairCd(), SimulatedQuote.fromPair(pair, lastOneSecondBar(pair)));
		}
	}

	/**
	 * Steps every pair once and returns the new ticks.
	 *
	 * @return one message per catalog pair
	 */
	public List<FxQuoteMessage> stepAll() {
		ensureLoaded();
		List<FxQuoteMessage> messages = new ArrayList<>(quotes.size());
		for (SimulatedQuote quote : quotes.values()) {
			quote.step();
			messages.add(quote.toMessage());
		}
		return messages;
	}

	/**
	 * Current snapshot without stepping.
	 *
	 * @return one message per catalog pair
	 */
	public List<FxQuoteMessage> snapshot() {
		ensureLoaded();
		List<FxQuoteMessage> messages = new ArrayList<>(quotes.size());
		for (SimulatedQuote quote : quotes.values()) {
			messages.add(quote.toMessage());
		}
		return messages;
	}

	private void ensureLoaded() {
		if (quotes.isEmpty()) {
			loadFromWarehouse();
		}
	}

	private CachedChartBar lastOneSecondBar(CurrencyPairDto pair) {
		List<CachedChartBar> bars = chartCacheStore.query(
				CacheNamespace.CACHE_SET_1S,
				pair.curpairName(),
				null,
				null);
		if (bars.isEmpty()) {
			return null;
		}
		return bars.get(bars.size() - 1);
	}

	static final class SimulatedQuote {

		private final int curpairCd;
		private final int scale;
		private final BigDecimal spread;
		private final BigDecimal maxStep;
		private BigDecimal bid;
		private BigDecimal ask;
		private BigDecimal mid;
		private BigDecimal high;
		private BigDecimal low;

		private SimulatedQuote(
				int curpairCd,
				int scale,
				BigDecimal spread,
				BigDecimal maxStep,
				BigDecimal bid) {
			this.curpairCd = curpairCd;
			this.scale = scale;
			this.spread = spread;
			this.maxStep = maxStep;
			this.bid = bid;
			applyAskFromBid();
			this.high = ask;
			this.low = bid;
		}

		static SimulatedQuote fromPair(CurrencyPairDto pair, CachedChartBar lastBar) {
			boolean yenQuote = pair.curpairName().endsWith("JPY");
			int scale = yenQuote ? 3 : 5;
			BigDecimal spread = BigDecimal.valueOf(DemoMarket.fullSpread(pair.curpairName()))
					.setScale(scale, RoundingMode.HALF_UP);
			BigDecimal bid = startingBid(pair, lastBar, scale);
			return new SimulatedQuote(pair.curpairCd(), scale, spread, maxStep(pair, yenQuote, scale), bid);
		}

		private static BigDecimal startingBid(CurrencyPairDto pair, CachedChartBar lastBar, int scale) {
			if (lastBar != null) {
				return BigDecimal.valueOf(lastBar.bidClose()).setScale(scale, RoundingMode.HALF_UP);
			}
			return BigDecimal.valueOf(DemoMarket.seedBid(pair.curpairName()))
					.setScale(scale, RoundingMode.HALF_UP);
		}

		private static BigDecimal maxStep(CurrencyPairDto pair, boolean yenQuote, int scale) {
			return switch (pair.curpairName()) {
				case "USDJPY" -> bd("0.012", scale);
				case "EURJPY" -> bd("0.014", scale);
				case "EURUSD" -> bd("0.00012", scale);
				case "GBPUSD" -> bd("0.00014", scale);
				case "AUDUSD" -> bd("0.00010", scale);
				default -> yenQuote ? bd("0.010", scale) : bd("0.00010", scale);
			};
		}

		void step() {
			double gaussian = ThreadLocalRandom.current().nextGaussian();
			BigDecimal delta = maxStep.multiply(BigDecimal.valueOf(gaussian / 3.0));
			bid = bid.add(delta).setScale(scale, RoundingMode.HALF_UP);
			if (bid.compareTo(BigDecimal.ZERO) <= 0) {
				bid = maxStep;
			}
			applyAskFromBid();
			if (ask.compareTo(high) > 0) {
				high = ask;
			}
			if (bid.compareTo(low) < 0) {
				low = bid;
			}
		}

		FxQuoteMessage toMessage() {
			return new FxQuoteMessage(
					String.valueOf(curpairCd),
					System.currentTimeMillis(),
					bid.doubleValue(),
					ask.doubleValue(),
					mid.doubleValue(),
					high.doubleValue(),
					low.doubleValue());
		}

		private void applyAskFromBid() {
			ask = bid.add(spread).setScale(scale, RoundingMode.HALF_UP);
			if (bid.compareTo(ask) >= 0) {
				ask = bid.add(BigDecimal.ONE.movePointLeft(scale));
			}
			mid = bid.add(ask).divide(BigDecimal.TWO, scale, RoundingMode.HALF_UP);
		}

		private static BigDecimal bd(String value, int scale) {
			return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
		}
	}
}
