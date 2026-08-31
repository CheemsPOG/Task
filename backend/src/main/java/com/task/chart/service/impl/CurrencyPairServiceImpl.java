/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.dto.response.CurrencyPairDto;
import com.task.chart.entity.Ccypair;
import com.task.chart.repository.CcypairRepository;
import com.task.chart.service.CurrencyPairService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CurrencyPairService}.
 *
 * <p>{@code GET /curpairs} is mapped from design-doc {@code m_ccypairs} (123 / 124):
 * {@code curpairCd} = {@code priority}, {@code curpairName} = {@code ccypair_cd},
 * {@code curpairDisplay} = slash form ({@code USDJPY} → {@code USD/JPY}).
 * {@link com.task.chart.controller.CurrencyPairController} and {@code TickIngestWorker} call
 * {@link #list()}.
 *
 * <p><strong>NOT:</strong> not doc 123 {@code GET /api/symbols}; not Peach S-01; not the
 * Python WS. {@code GET /curpairs} lives <em>outside</em> {@code /api} but still needs a
 * Bearer JWT ({@code SecurityConfig}). {@code curpairCd} is master {@code priority} — that
 * is the id Python publishes as a <em>string</em> on {@code peach:quotes}.
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Read catalog from m_ccypairs</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 *   <tr><td>1.1.2</td><td>2026/08/31</td><td>Task</td><td>Review comments: priority vs CD</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.2
 */
@Service
public class CurrencyPairServiceImpl implements CurrencyPairService {

	private final CcypairRepository ccypairRepository;

	/**
	 * Creates the service.
	 *
	 * @param ccypairRepository currency pair master
	 */
	public CurrencyPairServiceImpl(CcypairRepository ccypairRepository) {
		this.ccypairRepository = ccypairRepository;
	}

	/**
	 * Active rows only ({@code is_deleted = 0}), ordered by {@code priority}. Soft-deleted
	 * pairs stay out of the widget catalog and the ingest loop.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<CurrencyPairDto> list() {
		List<Ccypair> rows = ccypairRepository.findByIsDeletedOrderByPriorityAsc(Ccypair.ACTIVE);
		return rows.stream().map(CurrencyPairServiceImpl::toDto).toList();
	}

	/**
	 * Lookup by numeric quote-stream id ({@code priority}), not by {@code USDJPY}.
	 * Returns {@code null} on miss — callers decide 404 vs skip.
	 */
	@Override
	@Transactional(readOnly = true)
	public CurrencyPairDto find(int curpairCd) {
		return ccypairRepository
				.findFirstByPriorityAndIsDeletedOrderByCcypairCdAsc(curpairCd, Ccypair.ACTIVE)
				.map(CurrencyPairServiceImpl::toDto)
				.orElse(null);
	}

	/**
	 * JSON {@code curpairCd} = {@code priority} (Python tick id). {@code curpairName} =
	 * {@code ccypair_cd}. Do not swap those two fields — the WS would key quotes wrong.
	 */
	private static CurrencyPairDto toDto(Ccypair row) {
		String name = row.getCcypairCd();
		return new CurrencyPairDto(row.getPriority(), name, toDisplay(name));
	}

	/**
	 * {@code USDJPY} → {@code USD/JPY} for the header catalog. Non-6-char values pass through.
	 */
	private static String toDisplay(String ccypairCd) {
		if (ccypairCd == null || ccypairCd.length() != 6) {
			return ccypairCd;
		}

		return ccypairCd.substring(0, 3) + "/" + ccypairCd.substring(3);
	}
}
