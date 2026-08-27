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
 * {@link #list()}. This is NOT doc 123 {@code GET /api/symbols}, NOT Peach S-01, and NOT the Python WS.
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
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
	 * Returns active pairs for the quote-stream catalog.
	 *
	 * @return catalog rows
	 */
	@Override
	@Transactional(readOnly = true)
	public List<CurrencyPairDto> list() {
		List<Ccypair> rows = ccypairRepository.findByIsDeletedOrderByPriorityAsc(Ccypair.ACTIVE);
		return rows.stream().map(CurrencyPairServiceImpl::toDto).toList();
	}

	/**
	 * Looks up one catalog row by numeric {@code curpairCd} (master {@code priority}).
	 *
	 * @param curpairCd quote-stream pair code
	 * @return matching row, or {@code null}
	 */
	@Override
	@Transactional(readOnly = true)
	public CurrencyPairDto find(int curpairCd) {
		return ccypairRepository
				.findFirstByPriorityAndIsDeletedOrderByCcypairCdAsc(curpairCd, Ccypair.ACTIVE)
				.map(CurrencyPairServiceImpl::toDto)
				.orElse(null);
	}

	private static CurrencyPairDto toDto(Ccypair row) {
		String name = row.getCcypairCd();
		return new CurrencyPairDto(row.getPriority(), name, toDisplay(name));
	}

	private static String toDisplay(String ccypairCd) {
		if (ccypairCd == null || ccypairCd.length() != 6) {
			return ccypairCd;
		}

		return ccypairCd.substring(0, 3) + "/" + ccypairCd.substring(3);
	}
}
