package com.task.chart.dto;

public record FxQuoteMessage(
		String curpairCd,
		long rateMiliSecondUTC,
		double bid,
		double ask,
		double mid,
		double high,
		double low) {
}
