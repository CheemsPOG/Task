/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.BarDto;
import com.task.chart.service.MockBarGenerator;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import com.task.chart.util.DemoMarket;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link MockBarGenerator}.
 */
@Service
public class MockBarGeneratorImpl implements MockBarGenerator {

	@Override
	public List<BarDto> generate(CachedSymbol symbol, long periodMs, long toMs, int countBack) {
		return generate(symbol, periodMs, toMs, countBack, PriceComponent.MID);
	}

	@Override
	public List<BarDto> generate(
			CachedSymbol symbol,
			long periodMs,
			long toMs,
			int countBack,
			PriceComponent price) {
		long lastOpen = Math.floorDiv(toMs - 1, periodMs) * periodMs;
		List<BarDto> bars = new ArrayList<>(countBack);
		for (int i = countBack - 1; i >= 0; i--) {
			long time = lastOpen - (long) i * periodMs;
			if (time < 0) {
				continue;
			}
			BarDto bar = barAt(symbol, periodMs, time, price);
			if (bar.time() < toMs) {
				bars.add(bar);
			}
		}
		return bars;
	}

	@Override
	public BarDto barAt(CachedSymbol symbol, long periodMs, long time) {
		return barAt(symbol, periodMs, time, PriceComponent.MID);
	}

	@Override
	public BarDto barAt(CachedSymbol symbol, long periodMs, long time, PriceComponent price) {
		PriceComponent component = price == null ? PriceComponent.MID : price;
		int scale = symbol.priceScale();
		double open = round(priceAt(symbol, periodMs, time, component), scale);
		double close = round(priceAt(symbol, periodMs, time + periodMs, component), scale);
		double p1 = round(priceAt(symbol, periodMs, time + periodMs / 3, component), scale);
		double p2 = round(priceAt(symbol, periodMs, time + 2 * periodMs / 3, component), scale);
		double high = max(open, close, p1, p2);
		double low = min(open, close, p1, p2);
		String name = symbol.providerSymbol();
		long mix = mix64(symbol.ticker().hashCode() + (long) component.ordinal() * 97L, time / periodMs);
		double inner = DemoMarket.barAmplitude(name) * (0.15 + 0.20 * ((mix & 1023) / 1023.0));
		double outer = DemoMarket.outerWick(name);
		if (component == PriceComponent.ASK) {
			high = round(high + outer, scale);
			low = round(low - inner, scale);
		} else if (component == PriceComponent.BID) {
			high = round(high + inner, scale);
			low = round(low - outer, scale);
		} else {
			high = round(high + inner, scale);
			low = round(low - inner, scale);
		}
		if (low <= 0) {
			low = round(Math.min(open, close) * 0.5, scale);
		}
		double volume = 80 + (mix & 2047);
		return new BarDto(time, open, high, low, close, volume);
	}

	private static double priceAt(
			CachedSymbol symbol,
			long periodMs,
			long time,
			PriceComponent price) {
		double bid = bidAt(symbol, periodMs, time);
		String name = symbol.providerSymbol();
		return switch (price) {
			case BID -> bid;
			case ASK -> bid + DemoMarket.fullSpread(name);
			case MID -> bid + DemoMarket.halfSpread(name);
		};
	}

	private static double bidAt(CachedSymbol symbol, long periodMs, long time) {
		double seed = DemoMarket.seedBid(symbol.providerSymbol());
		long steps = Math.floorDiv(time, periodMs);
		int hash = symbol.ticker().hashCode();
		double wave = Math.sin((steps + hash) * 0.013) * 0.004
				+ Math.sin((steps + hash) * 0.0031) * 0.008
				+ Math.sin((steps + hash) * 0.0007) * 0.012;
		double noise = ((mix64(hash, steps) & 0xffff) / 65535.0 - 0.5) * 0.003;
		return Math.max(seed * (1.0 + wave + noise), seed * 0.2);
	}

	private static double max(double a, double b, double c, double d) {
		return Math.max(Math.max(a, b), Math.max(c, d));
	}

	private static double min(double a, double b, double c, double d) {
		return Math.min(Math.min(a, b), Math.min(c, d));
	}

	private static double round(double value, int priceScale) {
		return Math.round(value * priceScale) / (double) priceScale;
	}

	private static long mix64(long a, long b) {
		long x = a * 0x9E3779B97F4A7C15L ^ b;
		x ^= x >>> 33;
		x *= 0xff51afd7ed558ccdL;
		x ^= x >>> 33;
		return x;
	}
}
