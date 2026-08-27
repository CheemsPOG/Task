/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.util;

/**
 * Shared demo FX parameters for bars and quotes.
 *
 * <p>Seed BID, spread, and price scale for mock OHLC ({@link com.task.chart.service.impl.MockBarGeneratorImpl})
 * and the mock LP walk ({@link com.task.chart.cache.DemoTickEngine}). JPY pairs use scale 1000 and
 * wider ticks; others use 100000. This is NOT a live Peach feed, NOT the Python WS, and NOT
 * {@code m_ccypairs} rates.
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public final class DemoMarket {

	private DemoMarket() {
	}

	/**
	 * Price scale used by the TradingView symbol info (JPY pairs use 1000).
	 *
	 * @param curpairName pair CD such as {@code USDJPY}
	 * @return 1000 for JPY quotes, otherwise 100000
	 */
	public static int priceScale(String curpairName) {
		return yenQuote(curpairName) ? 1000 : 100_000;
	}

	/**
	 * Starting BID used by the mock bar and quote generators.
	 *
	 * @param curpairName pair CD
	 * @return seed BID
	 */
	public static double seedBid(String curpairName) {
		return seedMid(curpairName) - halfSpread(curpairName);
	}

	/**
	 * Starting MID used by the mock generators.
	 *
	 * @param curpairName pair CD
	 * @return seed MID
	 */
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

	/**
	 * Inner bar amplitude for mock OHLC.
	 *
	 * @param curpairName pair CD
	 * @return amplitude in price units
	 */
	public static double barAmplitude(String curpairName) {
		return yenQuote(curpairName) ? 0.08 : 0.00080;
	}

	/**
	 * Half of the BID/ASK spread.
	 *
	 * @param curpairName pair CD
	 * @return half spread
	 */
	public static double halfSpread(String curpairName) {
		return yenQuote(curpairName) ? 0.05 : 0.00050;
	}

	/**
	 * Full BID/ASK spread ({@code ASK = BID + spread}).
	 *
	 * @param curpairName pair CD
	 * @return full spread
	 */
	public static double fullSpread(String curpairName) {
		return halfSpread(curpairName) * 2.0;
	}

	/**
	 * Outer wick size for mock OHLC.
	 *
	 * @param curpairName pair CD
	 * @return wick size
	 */
	public static double outerWick(String curpairName) {
		return yenQuote(curpairName) ? 0.20 : 0.0020;
	}

	private static boolean yenQuote(String curpairName) {
		return curpairName != null && curpairName.endsWith("JPY");
	}
}
