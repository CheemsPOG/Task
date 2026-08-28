/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.List;

/**
 * Historical bar payload for GET /api/history (design doc 121).
 *
 * <p>{@code ChartDataServiceImpl} fills this from Redis {@code ChartCacheStore}. The JSON body
 * is Peach columnar arrays only: {@code s}, {@code t}, {@code o}, {@code h}, {@code l}, {@code c},
 * and {@code nextTime} on {@code no_data}. {@code t} is unix seconds. Widget {@code Bar[]} objects
 * are reconstructed in the frontend datafeed. It is not the live forming-bar bus
 * ({@link FormingBarMessage}) and not a warehouse row.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>nextTime + Peach columnar arrays</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/27</td><td>Task</td><td>Doc 121 fields only (drop bars/noData/errmsg)</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
 */
@JsonInclude(Include.NON_NULL)
public record HistoryResponse(
		String s,
		List<Long> t,
		List<Double> o,
		List<Double> h,
		List<Double> l,
		List<Double> c,
		Long nextTime) {

	/**
	 * Success payload with Peach columnar arrays ({@code t} in unix seconds).
	 *
	 * @param bars internal OHLCV bars ({@code time} in milliseconds)
	 * @return response
	 */
	public static HistoryResponse ok(List<BarDto> bars) {
		Columnar columnar = Columnar.fromBars(bars);
		return new HistoryResponse(
				"ok",
				columnar.t(),
				columnar.o(),
				columnar.h(),
				columnar.l(),
				columnar.c(),
				null);
	}

	/**
	 * Empty range ({@code s=no_data}) with optional Peach {@code nextTime} (unix seconds).
	 *
	 * @param nextTimeSeconds latest bar open before {@code from}, or {@code null}
	 * @return response
	 */
	public static HistoryResponse empty(Long nextTimeSeconds) {
		return new HistoryResponse(
				"no_data",
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				nextTimeSeconds);
	}

	/**
	 * Empty range without {@code nextTime}.
	 *
	 * @return response
	 */
	public static HistoryResponse empty() {
		return empty(null);
	}

	private record Columnar(
			List<Long> t,
			List<Double> o,
			List<Double> h,
			List<Double> l,
			List<Double> c) {

		static Columnar fromBars(List<BarDto> bars) {
			List<Long> times = new ArrayList<>(bars.size());
			List<Double> opens = new ArrayList<>(bars.size());
			List<Double> highs = new ArrayList<>(bars.size());
			List<Double> lows = new ArrayList<>(bars.size());
			List<Double> closes = new ArrayList<>(bars.size());
			for (BarDto bar : bars) {

				// Widget bars use milliseconds; Peach columnar t is unix seconds.
				times.add(bar.time() / 1000L);
				opens.add(bar.open());
				highs.add(bar.high());
				lows.add(bar.low());
				closes.add(bar.close());
			}
			return new Columnar(times, opens, highs, lows, closes);
		}
	}
}
