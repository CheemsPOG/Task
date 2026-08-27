/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.service.CurrencyPairService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FX pair catalog HTTP API used to map WebSocket quotes. Extra vs design docs
 * 120–139 (same {@code m_ccypairs} master as docs 123 / 124). HTTP only; no SQL.
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Catalog from m_ccypairs</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/24</td><td>Task</td><td>Require JWT (S-01 stand-in)</td></tr>
 *   <tr><td>1.3.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.3.0
 */
@RestController
@Tag(name = "Currency pairs", description = "m_ccypairs catalog for mapping WebSocket curpairCd")
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
	 * Returns the FX pair list used to map WebSocket {@code curpairCd} (string) to a pair.
	 * Rows come from {@code m_ccypairs} (design docs 123 / 124). Extra vs 120–139.
	 *
	 * @return catalog rows {@code curpairCd}, {@code curpairName}, {@code curpairDisplay}
	 */
	@GetMapping("/curpairs")
	@Operation(
			summary = "List FX pairs for quote mapping",
			description = "Same master as GET /api/symbols and /api/search (m_ccypairs). "
					+ "Map WebSocket curpairCd (string) to curpairCd (number) here. Requires Bearer JWT.")
	@ApiResponse(responseCode = "200", description = "Catalog for the quote stream")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
	public List<CurrencyPairDto> curpairs() {

		// Extra vs 120–139; same m_ccypairs master as docs 123 / 124.
		return currencyPairService.list();
	}
}
