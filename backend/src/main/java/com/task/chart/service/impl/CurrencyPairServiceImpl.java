/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.service.CurrencyPairService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link CurrencyPairService}.
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
@Service
public class CurrencyPairServiceImpl implements CurrencyPairService {

	private static final List<CurrencyPairDto> PAIRS = List.of(
			new CurrencyPairDto(1, "USDJPY", "USD/JPY"),
			new CurrencyPairDto(2, "EURJPY", "EUR/JPY"),
			new CurrencyPairDto(3, "EURUSD", "EUR/USD"),
			new CurrencyPairDto(4, "GBPUSD", "GBP/USD"),
			new CurrencyPairDto(5, "AUDUSD", "AUD/USD"));

	private final Map<Integer, CurrencyPairDto> byCode = PAIRS.stream()
			.collect(Collectors.toUnmodifiableMap(CurrencyPairDto::curpairCd, Function.identity()));

	/**
	 * Returns the hardcoded demo pair catalog (same five pairs as {@code m_ccypairs}).
	 *
	 * @return catalog rows
	 */
	@Override
	public List<CurrencyPairDto> list() {
		return PAIRS;
	}

	/**
	 * Looks up one catalog row by numeric {@code curpairCd}.
	 *
	 * @param curpairCd quote-stream pair code
	 * @return matching row, or {@code null}
	 */
	@Override
	public CurrencyPairDto find(int curpairCd) {
		return byCode.get(curpairCd);
	}
}
