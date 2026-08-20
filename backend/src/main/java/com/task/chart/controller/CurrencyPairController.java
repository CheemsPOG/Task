package com.task.chart.controller;

import com.task.chart.dto.CurrencyPairDto;
import com.task.chart.service.CurrencyPairService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyPairController {

	private final CurrencyPairService currencyPairService;

	public CurrencyPairController(CurrencyPairService currencyPairService) {
		this.currencyPairService = currencyPairService;
	}

	@GetMapping("/curpairs")
	public List<CurrencyPairDto> curpairs() {
		return currencyPairService.list();
	}
}
