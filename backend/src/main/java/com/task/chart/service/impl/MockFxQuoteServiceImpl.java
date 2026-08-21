/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.dto.response.FxQuoteMessage;
import com.task.chart.service.CurrencyPairService;
import com.task.chart.service.MockFxQuoteService;
import com.task.chart.util.DemoMarket;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link MockFxQuoteService}.
 */
@Service
public class MockFxQuoteServiceImpl implements MockFxQuoteService {

	private final CurrencyPairService currencyPairService;
	private final Map<Integer, SimulatedQuote> quotes = new ConcurrentHashMap<>();
	private final List<QuoteListener> listeners = new CopyOnWriteArrayList<>();

	public MockFxQuoteServiceImpl(CurrencyPairService currencyPairService) {
		this.currencyPairService = currencyPairService;
		for (CurrencyPairDto pair : currencyPairService.list()) {
			quotes.put(pair.curpairCd(), SimulatedQuote.seed(pair));
		}
	}

	@Override
	public void addListener(QuoteListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(QuoteListener listener) {
		listeners.remove(listener);
	}

	@Override
	public List<FxQuoteMessage> snapshot() {
		List<FxQuoteMessage> messages = new ArrayList<>(quotes.size());
		quotes.values().forEach(quote -> messages.add(quote.toMessage()));
		return messages;
	}

	@Override
	public double currentMid(int curpairCd) {
		return currentPrice(curpairCd, PriceComponent.MID);
	}

	@Override
	public double currentPrice(int curpairCd, PriceComponent price) {
		SimulatedQuote quote = quotes.get(curpairCd);
		if (quote == null) {
			throw new IllegalArgumentException("unknown pair " + curpairCd);
		}
		return switch (price == null ? PriceComponent.MID : price) {
			case BID -> quote.bidValue();
			case ASK -> quote.askValue();
			case MID -> quote.midValue();
		};
	}

	@Override
	@Scheduled(fixedRate = 333)
	public void tick() {
		List<FxQuoteMessage> messages = new ArrayList<>(quotes.size());
		quotes.values().forEach(quote -> {
			quote.step();
			messages.add(quote.toMessage());
		});
		if (listeners.isEmpty()) {
			return;
		}
		listeners.forEach(listener -> listener.onQuotes(messages));
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

		static SimulatedQuote seed(CurrencyPairDto pair) {
			boolean yenQuote = pair.curpairName().endsWith("JPY");
			int scale = yenQuote ? 3 : 5;
			BigDecimal spread = BigDecimal.valueOf(DemoMarket.fullSpread(pair.curpairName()))
					.setScale(scale, RoundingMode.HALF_UP);
			BigDecimal bid = BigDecimal.valueOf(DemoMarket.seedBid(pair.curpairName()))
					.setScale(scale, RoundingMode.HALF_UP);
			BigDecimal maxStep = switch (pair.curpairName()) {
				case "USDJPY" -> bd("0.012", scale);
				case "EURJPY" -> bd("0.014", scale);
				case "EURUSD" -> bd("0.00012", scale);
				case "GBPUSD" -> bd("0.00014", scale);
				case "AUDUSD" -> bd("0.00010", scale);
				default -> yenQuote ? bd("0.010", scale) : bd("0.00010", scale);
			};
			return new SimulatedQuote(pair.curpairCd(), scale, spread, maxStep, bid);
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

		double midValue() {
			return mid.doubleValue();
		}

		double bidValue() {
			return bid.doubleValue();
		}

		double askValue() {
			return ask.doubleValue();
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
				ask = bid.add(tick());
			}
			mid = bid.add(ask).divide(BigDecimal.TWO, scale, RoundingMode.HALF_UP);
		}

		private BigDecimal tick() {
			return BigDecimal.ONE.movePointLeft(scale);
		}

		private static BigDecimal bd(String value, int scale) {
			return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
		}
	}
}
