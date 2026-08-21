/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.service.CurrencyPairService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint that returns the mock FX pair catalog.
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
@RestController
public class CurrencyPairController {

	private final CurrencyPairService currencyPairService;

	/**
	 * Creates the controller.
	 *
	 * @param currencyPairService pair catalog
	 */
	public CurrencyPairController(CurrencyPairService currencyPairService) {
		this.currencyPairService = currencyPairService;
	}

	/**
	 * Returns the mock FX pair list used to map WebSocket {@code curpairCd} values.
	 *
	 * @return catalog rows
	 */
	@GetMapping("/curpairs")
	public List<CurrencyPairDto> curpairs() {
		return currencyPairService.list();
	}
}
