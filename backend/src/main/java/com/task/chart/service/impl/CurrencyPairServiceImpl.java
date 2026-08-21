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

	@Override
	public List<CurrencyPairDto> list() {
		return PAIRS;
	}

	@Override
	public CurrencyPairDto find(int curpairCd) {
		return byCode.get(curpairCd);
	}
}
