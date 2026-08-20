package com.task.chart.service;

import com.task.chart.dto.BarDto;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MockBarGenerator {

	public List<BarDto> generate(CachedSymbol symbol, long periodMs, long toMs, int countBack) {
		long lastOpen = Math.floorDiv(toMs - 1, periodMs) * periodMs;
		List<BarDto> bars = new ArrayList<>(countBack);
		for (int i = countBack - 1; i >= 0; i--) {
			long time = lastOpen - (long) i * periodMs;
			if (time < 0) {
				continue;
			}
			BarDto bar = barAt(symbol, periodMs, time);
			if (bar.time() < toMs) {
				bars.add(bar);
			}
		}
		return bars;
	}

	public BarDto barAt(CachedSymbol symbol, long periodMs, long time) {
		double open = round(midAt(symbol, periodMs, time), symbol.priceScale());
		double close = round(midAt(symbol, periodMs, time + periodMs), symbol.priceScale());
		double amplitude = DemoMarket.barAmplitude(symbol.providerSymbol());
		long mix = mix64(symbol.ticker().hashCode(), time / periodMs);
		double highExtra = amplitude * ((mix & 1023) / 1023.0);
		double lowExtra = amplitude * (((mix >> 10) & 1023) / 1023.0);
		double high = round(Math.max(open, close) + highExtra, symbol.priceScale());
		double low = round(Math.min(open, close) - lowExtra, symbol.priceScale());
		if (low <= 0) {
			low = round(Math.min(open, close) * 0.5, symbol.priceScale());
		}
		double volume = 80 + (mix & 2047);
		return new BarDto(time, open, high, low, close, volume);
	}

	private static double midAt(CachedSymbol symbol, long periodMs, long time) {
		double seed = DemoMarket.seedMid(symbol.providerSymbol());
		long steps = Math.floorDiv(time, periodMs);
		int hash = symbol.ticker().hashCode();
		double wave = Math.sin((steps + hash) * 0.013) * 0.004
				+ Math.sin((steps + hash) * 0.0031) * 0.008
				+ Math.sin((steps + hash) * 0.0007) * 0.012;
		double noise = ((mix64(hash, steps) & 0xffff) / 65535.0 - 0.5) * 0.003;
		return Math.max(seed * (1.0 + wave + noise), seed * 0.2);
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
