/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

import com.task.chart.constants.PriceComponent;
import com.task.chart.dto.response.BarDto;
import com.task.chart.service.SymbolCatalog.CachedSymbol;
import java.util.List;

/**
 * Deterministic mock OHLCV generator for chart history.
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
public interface MockBarGenerator {

	List<BarDto> generate(CachedSymbol symbol, long periodMs, long toMs, int countBack);

	List<BarDto> generate(
			CachedSymbol symbol,
			long periodMs,
			long toMs,
			int countBack,
			PriceComponent price);

	BarDto barAt(CachedSymbol symbol, long periodMs, long time);

	BarDto barAt(CachedSymbol symbol, long periodMs, long time, PriceComponent price);
}
