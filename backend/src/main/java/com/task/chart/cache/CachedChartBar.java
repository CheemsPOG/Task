/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.BarDto;

/**
 * One cached OHLC row with bid and ask sides (design doc 121 bar mapping source).
 *
 * <p>{@link TickIngestWorker} uses {@link #openFromTick} / {@link #applyTick} to
 * write the forming candle. {@code GET /api/history} maps this row to
 * {@link BarDto} via {@link #toBarDto}. MID is averaged at read time.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.1.0</td><td>2026/08/26</td><td>Task</td><td>applyTick for ingest open bars</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
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

	/**
	 * Open price for the requested side.
	 *
	 * @param side BID, ASK, or MID
	 * @return open
	 */
	public double open(PriceComponent side) {

		return switch (side) {
			case BID -> bidOpen;
			case ASK -> askOpen;
			case MID -> mid(bidOpen, askOpen);
		};
	}

	/**
	 * High price for the requested side.
	 *
	 * @param side BID, ASK, or MID
	 * @return high
	 */
	public double high(PriceComponent side) {

		return switch (side) {
			case BID -> bidHigh;
			case ASK -> askHigh;
			case MID -> mid(bidHigh, askHigh);
		};
	}

	/**
	 * Low price for the requested side.
	 *
	 * @param side BID, ASK, or MID
	 * @return low
	 */
	public double low(PriceComponent side) {

		return switch (side) {
			case BID -> bidLow;
			case ASK -> askLow;
			case MID -> mid(bidLow, askLow);
		};
	}

	/**
	 * Close price for the requested side.
	 *
	 * @param side BID, ASK, or MID
	 * @return close
	 */
	public double close(PriceComponent side) {

		return switch (side) {
			case BID -> bidClose;
			case ASK -> askClose;
			case MID -> mid(bidClose, askClose);
		};
	}

	/**
	 * Opens a new bar from the current BID/ASK tick.
	 *
	 * @param curpairCd warehouse pair CD
	 * @param chartDatetimeSec bar open unix seconds
	 * @param bid current BID
	 * @param ask current ASK
	 * @return new row
	 */
	public static CachedChartBar openFromTick(String curpairCd, long chartDatetimeSec, double bid, double ask) {
		return new CachedChartBar(
				curpairCd,
				chartDatetimeSec,
				bid,
				bid,
				bid,
				bid,
				ask,
				ask,
				ask,
				ask,
				1);
	}

	/**
	 * Updates high/low/close from a live tick; volume increments by one.
	 *
	 * @param bid current BID
	 * @param ask current ASK
	 * @return updated row
	 */
	public CachedChartBar applyTick(double bid, double ask) {
		return new CachedChartBar(
				curpairCd,
				chartDatetimeSec,
				bidOpen,
				Math.max(bidHigh, bid),
				Math.min(bidLow, bid),
				bid,
				askOpen,
				Math.max(askHigh, ask),
				Math.min(askLow, ask),
				ask,
				volume + 1);
	}

	/**
	 * Mid price used when {@code bid_ask=MID}.
	 *
	 * @param bid bid value
	 * @param ask ask value
	 * @return average
	 */
	private static double mid(double bid, double ask) {
		return (bid + ask) / 2.0;
	}
}
