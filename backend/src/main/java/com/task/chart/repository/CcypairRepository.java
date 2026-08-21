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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
public interface CcypairRepository extends JpaRepository<Ccypair, String> {

	Optional<Ccypair> findByCcypairCdAndIsDeleted(String ccypairCd, int isDeleted);

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
