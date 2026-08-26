/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.dto.response.CurrencyPairDto;
import java.util.List;

/**
 * Quote-stream catalog mapped from {@code m_ccypairs} (design docs 123 / 124).
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
public interface CurrencyPairService {

	/**
	 * Returns the quote-stream catalog from {@code m_ccypairs}
	 * ({@code curpairCd} = priority, {@code curpairName} = ccypair_cd).
	 *
	 * @return catalog rows
	 */
	List<CurrencyPairDto> list();

	/**
	 * Looks up one catalog row by numeric {@code curpairCd} ({@code m_ccypairs.priority}).
	 *
	 * @param curpairCd quote-stream pair code
	 * @return matching row, or {@code null}
	 */
	CurrencyPairDto find(int curpairCd);
}
