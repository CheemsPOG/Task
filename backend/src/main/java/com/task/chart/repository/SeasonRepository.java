/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.repository;

import com.task.chart.entity.Season;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Season master access.
 *
 * <p>Spring Data JPA for {@code m_season}. {@code ChartDataServiceImpl.currentSession} calls it
 * so design doc 123 can pick summer vs winter session text. Callers pass now for both window
 * bounds. It is not a holiday calendar and not the bar warehouse.
 *
 * <p>Demo seeds one winter row covering 2020–2099. A caller outside that window gets
 * {@code ServerErrorException} (500), not a graceful fallback.
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public interface SeasonRepository extends JpaRepository<Season, Long> {

	/**
	 * Seasons whose code is in {@code seasonCds} and whose window covers both instants.
	 *
	 * @param seasonCds daylight-saving and/or standard codes
	 * @param startAt inclusive window start (typically now)
	 * @param endAt inclusive window end (typically now)
	 * @return matching rows, newest {@code start_at} first
	 */
	List<Season> findBySeasonCdInAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtDesc(
			Collection<Integer> seasonCds,
			Instant startAt,
			Instant endAt);
}
