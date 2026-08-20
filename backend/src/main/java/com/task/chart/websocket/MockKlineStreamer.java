package com.task.chart.websocket;

import com.task.chart.dto.BarDto;
import com.task.chart.dto.FxQuoteMessage;
import com.task.chart.service.MockBarGenerator;
import com.task.chart.service.MockFxQuoteService;
import com.task.chart.service.PriceComponent;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Builds live candles from mock FX bid, ask, or mid so each price side has its own series.
 */
@Component
public class MockKlineStreamer implements MockFxQuoteService.QuoteListener {

	private final MockFxQuoteService mockFxQuoteService;
	private final MockBarGenerator mockBarGenerator;
	private final Map<String, Set<Consumer<BarDto>>> listenersByStream = new ConcurrentHashMap<>();
	private final Map<String, StreamState> states = new ConcurrentHashMap<>();

	public MockKlineStreamer(MockFxQuoteService mockFxQuoteService, MockBarGenerator mockBarGenerator) {
		this.mockFxQuoteService = mockFxQuoteService;
		this.mockBarGenerator = mockBarGenerator;
		mockFxQuoteService.addListener(this);
	}

	public void subscribe(CachedSymbol symbol, long periodMs, PriceComponent price, Consumer<BarDto> listener) {
		PriceComponent component = price == null ? PriceComponent.MID : price;
		String streamName = streamName(symbol, periodMs, component);
		listenersByStream.computeIfAbsent(streamName, key -> new CopyOnWriteArraySet<>()).add(listener);
		states.computeIfAbsent(streamName, key -> new StreamState(symbol, periodMs, component));
		BarDto current = currentBar(
				states.get(streamName),
				mockFxQuoteService.currentPrice(symbol.curpairCd(), component),
				System.currentTimeMillis());
		if (current != null) {
			listener.accept(current);
		}
	}

	public void unsubscribe(CachedSymbol symbol, long periodMs, PriceComponent price, Consumer<BarDto> listener) {
		PriceComponent component = price == null ? PriceComponent.MID : price;
		String streamName = streamName(symbol, periodMs, component);
		Set<Consumer<BarDto>> listeners = listenersByStream.get(streamName);
		if (listeners == null) {
			return;
		}
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			listenersByStream.remove(streamName);
			states.remove(streamName);
		}
	}

	@Override
	public void onQuotes(List<FxQuoteMessage> quotes) {
		if (states.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		for (StreamState state : states.values()) {
			FxQuoteMessage quote = quoteFor(quotes, state.symbol.curpairCd());
			if (quote == null) {
				continue;
			}
			BarDto bar = currentBar(state, priceOf(quote, state.price), now);
			Set<Consumer<BarDto>> listeners =
					listenersByStream.get(streamName(state.symbol, state.periodMs, state.price));
			if (bar == null || listeners == null) {
				continue;
			}
			listeners.forEach(listener -> listener.accept(bar));
		}
	}

	private BarDto currentBar(StreamState state, double close, long nowMs) {
		long barTime = Math.floorDiv(nowMs, state.periodMs) * state.periodMs;
		if (state.bar == null || state.bar.time() != barTime) {
			double open = state.bar != null ? state.bar.close() : seedOpen(state, barTime, close);
			state.bar = new BarDto(barTime, open, Math.max(open, close), Math.min(open, close), close, 1);
			return state.bar;
		}
		state.bar = new BarDto(
				barTime,
				state.bar.open(),
				Math.max(state.bar.high(), close),
				Math.min(state.bar.low(), close),
				close,
				state.bar.volume() + 1);
		return state.bar;
	}

	private double seedOpen(StreamState state, long barTime, double close) {
		BarDto historical = mockBarGenerator.barAt(state.symbol, state.periodMs, barTime, state.price);
		if (historical != null) {
			return historical.open();
		}
		return close;
	}

	private static double priceOf(FxQuoteMessage quote, PriceComponent price) {
		return switch (price) {
			case BID -> quote.bid();
			case ASK -> quote.ask();
			case MID -> quote.mid();
		};
	}

	private static FxQuoteMessage quoteFor(List<FxQuoteMessage> quotes, int curpairCd) {
		String code = String.valueOf(curpairCd);
		for (FxQuoteMessage quote : quotes) {
			if (code.equals(quote.curpairCd())) {
				return quote;
			}
		}
		return null;
	}

	private static String streamName(CachedSymbol symbol, long periodMs, PriceComponent price) {
		return symbol.ticker() + "|" + periodMs + "|" + price.wireName();
	}

	private static final class StreamState {
		private final CachedSymbol symbol;
		private final long periodMs;
		private final PriceComponent price;
		private BarDto bar;

		private StreamState(CachedSymbol symbol, long periodMs, PriceComponent price) {
			this.symbol = symbol;
			this.periodMs = periodMs;
			this.price = price;
		}
	}
}
