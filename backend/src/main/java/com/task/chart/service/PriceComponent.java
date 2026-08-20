package com.task.chart.service;

import java.util.Locale;

public enum PriceComponent {
	BID,
	ASK,
	MID;

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

	public String wireName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
