/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.repository;

import com.task.chart.entity.TvTimescaleMark;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TV timescale mark master access.
 *
 * <p>Spring Data JPA for {@code m_tv_timescale_mark} (design doc 126). {@code ChartDataServiceImpl}
 * queries by pair, resolution, and inclusive unix {@code from}/{@code to}. There is no customer
 * column. It is not series-mark access ({@link TvMarkRepository}).
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/21</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public interface TvTimescaleMarkRepository extends JpaRepository<TvTimescaleMark, String> {

	/**
	 * Timescale marks for one pair and resolution inside an inclusive unix-seconds window
	 * (design doc 126).
	 *
	 * @param ccypairCd 6-char pair code
	 * @param resolution TV resolution
	 * @param fromInclusive window start (unix seconds)
	 * @param toInclusive window end (unix seconds)
	 * @return rows ordered by {@code timescale_mark_at} ascending
	 */
	List<TvTimescaleMark>
			findByCcypairCdAndResolutionAndTimescaleMarkAtGreaterThanEqualAndTimescaleMarkAtLessThanEqualOrderByTimescaleMarkAtAsc(
					String ccypairCd,
					String resolution,
					long fromInclusive,
					long toInclusive);
}
