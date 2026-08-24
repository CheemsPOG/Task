/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.constants;

import java.util.Locale;

/**
 * BID, ASK, or MID price side for history and live bars.
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
public enum PriceComponent {
	BID,
	ASK,
	MID;

	/**
	 * Parses a free-form price side; blank or unknown becomes {@code MID}.
	 *
	 * @param raw widget or query value
	 * @return BID, ASK, or MID
	 */
	public static PriceComponent from(String raw) {
		if (raw == null || raw.isBlank()) {
			return MID;
		}
		return switch (raw.trim().toLowerCase(Locale.ROOT)) {
			case "bid" -> BID;
			case "ask" -> ASK;
			default -> MID;
		};
	}

	/**
	 * Parses design-doc {@code bid_ask}: {@code BID}, {@code MID}, or {@code ASK} only.
	 *
	 * @param raw bid_ask query value
	 * @return matching side
	 * @throws IllegalArgumentException when the value is not BID, MID, or ASK
	 */
	public static PriceComponent fromBidAsk(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("bid_ask is required");
		}
		return switch (raw.trim().toUpperCase(Locale.ROOT)) {
			case "BID" -> BID;
			case "ASK" -> ASK;
			case "MID" -> MID;
			default -> throw new IllegalArgumentException("invalid bid_ask: " + raw);
		};
	}

	/**
	 * @return lowercase wire name ({@code bid}, {@code ask}, {@code mid})
	 */
	public String wireName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
