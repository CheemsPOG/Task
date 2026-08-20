package com.task.chart.service;

/**
 * Shared demo FX parameters for the pair catalog, historical bars, and live quotes.
 */
public final class DemoMarket {

	private DemoMarket() {
	}

	public static int priceScale(String curpairName) {
		return yenQuote(curpairName) ? 1000 : 100_000;
	}

	public static double seedBid(String curpairName) {
		return seedMid(curpairName) - halfSpread(curpairName);
	}

	public static double seedMid(String curpairName) {
		return switch (curpairName) {
			case "USDJPY" -> 149.850;
			case "EURJPY" -> 162.420;
			case "EURUSD" -> 1.08540;
			case "GBPUSD" -> 1.27180;
			case "AUDUSD" -> 0.66250;
			default -> yenQuote(curpairName) ? 100.000 : 1.00000;
		};
	}

	public static double barAmplitude(String curpairName) {
		return yenQuote(curpairName) ? 0.08 : 0.00080;
	}

	public static double halfSpread(String curpairName) {
		return yenQuote(curpairName) ? 0.05 : 0.00050;
	}

	public static double fullSpread(String curpairName) {
		return halfSpread(curpairName) * 2.0;
	}

	public static double outerWick(String curpairName) {
		return yenQuote(curpairName) ? 0.20 : 0.0020;
	}

	private static boolean yenQuote(String curpairName) {
		return curpairName != null && curpairName.endsWith("JPY");
	}
}
