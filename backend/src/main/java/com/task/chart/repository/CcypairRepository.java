/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.repository;

import com.task.chart.entity.Ccypair;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Currency pair master access.
 *
 * <p>Spring Data JPA for {@code m_ccypairs}. {@code ChartDataServiceImpl} uses it for docs 123 /
 * 124; {@code CurrencyPairServiceImpl} for {@code GET /curpairs}; {@code ChartLayoutServiceImpl}
 * to validate layout symbols. It is not the JDBC {@code t_chart_*} warehouse
 * ({@code ChartBarRepository} in {@code cache}).
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Add active search for doc 124</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/24</td><td>Task</td><td>Add list for GET /curpairs</td></tr>
 *   <tr><td>1.2.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.1
 */
public interface CcypairRepository extends JpaRepository<Ccypair, String> {

	/**
	 * Active pair by 6-char CD (design docs 123 / 127 symbol check).
	 *
	 * @param ccypairCd 6-char pair code
	 * @param isDeleted deleted flag ({@link Ccypair#ACTIVE})
	 * @return matching row, if any
	 */
	Optional<Ccypair> findByCcypairCdAndIsDeleted(String ccypairCd, int isDeleted);

	/**
	 * Active pairs for {@code GET /curpairs}, ordered like design doc 124 (priority ascending).
	 *
	 * @param isDeleted deleted flag ({@link Ccypair#ACTIVE})
	 * @return active rows
	 */
	List<Ccypair> findByIsDeletedOrderByPriorityAsc(int isDeleted);

	/**
	 * Active pair whose {@code priority} is the quote-stream {@code curpairCd}.
	 *
	 * @param priority quote-stream pair code
	 * @param isDeleted deleted flag ({@link Ccypair#ACTIVE})
	 * @return matching row, if any
	 */
	Optional<Ccypair> findFirstByPriorityAndIsDeletedOrderByCcypairCdAsc(int priority, int isDeleted);

	/**
	 * Active pairs ordered by priority; optional partial match on CD or Japanese name.
	 *
	 * @param isDeleted deleted flag ({@link Ccypair#ACTIVE})
	 * @param queryEmpty true when the search text is blank (return all active)
	 * @param needle original trimmed query
	 * @param needleCd query with slashes removed (matches {@code USDJPY} when user types {@code USD/JPY})
	 * @param pageable limit via page size
	 * @return matching pairs
	 */
	@Query("""
			SELECT c FROM Ccypair c
			WHERE c.isDeleted = :isDeleted
			AND (
				:queryEmpty = true
				OR LOWER(c.ccypairCd) LIKE LOWER(CONCAT('%', :needle, '%'))
				OR LOWER(c.ccypairCd) LIKE LOWER(CONCAT('%', :needleCd, '%'))
				OR c.ccypairJp LIKE CONCAT('%', :needle, '%')
			)
			ORDER BY c.priority ASC
			""")
	List<Ccypair> searchActive(
			@Param("isDeleted") int isDeleted,
			@Param("queryEmpty") boolean queryEmpty,
			@Param("needle") String needle,
			@Param("needleCd") String needleCd,
			Pageable pageable);
}
