/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.BarDto;

/**
 * One cached OHLC row with bid and ask sides (design doc 121 Bar mapping source).
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/21</td><td>Task</td><td>新規作成</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
public record CachedChartBar(
		String curpairCd,
		long chartDatetimeSec,
		double bidOpen,
		double bidHigh,
		double bidLow,
		double bidClose,
		double askOpen,
		double askHigh,
		double askLow,
		double askClose,
		double volume) {

	/**
	 * Maps this cache row to a widget bar for the given {@code bid_ask} side.
	 *
	 * @param bidAsk BID, ASK, or MID
	 * @return bar with {@code time} in milliseconds
	 */
	public BarDto toBarDto(PriceComponent bidAsk) {
		PriceComponent side = bidAsk == null ? PriceComponent.MID : bidAsk;
		return new BarDto(
				chartDatetimeSec * 1000L,
				open(side),
				high(side),
				low(side),
				close(side),
				volume);
	}

	public double open(PriceComponent side) {
		return switch (side) {
			case BID -> bidOpen;
			case ASK -> askOpen;
			case MID -> mid(bidOpen, askOpen);
		};
	}

	public double high(PriceComponent side) {
		return switch (side) {
			case BID -> bidHigh;
			case ASK -> askHigh;
			case MID -> mid(bidHigh, askHigh);
		};
	}

	public double low(PriceComponent side) {
		return switch (side) {
			case BID -> bidLow;
			case ASK -> askLow;
			case MID -> mid(bidLow, askLow);
		};
	}

	public double close(PriceComponent side) {
		return switch (side) {
			case BID -> bidClose;
			case ASK -> askClose;
			case MID -> mid(bidClose, askClose);
		};
	}

	private static double mid(double bid, double ask) {
		return (bid + ask) / 2.0;
	}
}
