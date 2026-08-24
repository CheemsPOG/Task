/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.dto.response.CurrencyPairDto;
import java.util.List;

/**
 * In-memory catalog of demo FX pairs.
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
public interface CurrencyPairService {

	/**
	 * Returns the hardcoded demo pair catalog (same five pairs as {@code m_ccypairs}).
	 *
	 * @return catalog rows
	 */
	List<CurrencyPairDto> list();

	/**
	 * Looks up one catalog row by numeric {@code curpairCd}.
	 *
	 * @param curpairCd quote-stream pair code
	 * @return matching row, or {@code null}
	 */
	CurrencyPairDto find(int curpairCd);
}
