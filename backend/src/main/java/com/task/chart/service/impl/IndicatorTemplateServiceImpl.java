/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.config.CustomerContext;
import com.task.chart.dto.response.IndicatorTemplateListItemDto;
import com.task.chart.entity.TvIndicatorTemplate;
import com.task.chart.exception.ServerErrorException;
import com.task.chart.repository.TvIndicatorTemplateRepository;
import com.task.chart.service.IndicatorTemplateService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link IndicatorTemplateService}.
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@Service
public class IndicatorTemplateServiceImpl implements IndicatorTemplateService {

	private final TvIndicatorTemplateRepository tvIndicatorTemplateRepository;

	/**
	 * Creates the service.
	 *
	 * @param tvIndicatorTemplateRepository indicator template table
	 */
	public IndicatorTemplateServiceImpl(TvIndicatorTemplateRepository tvIndicatorTemplateRepository) {
		this.tvIndicatorTemplateRepository = tvIndicatorTemplateRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<IndicatorTemplateListItemDto> list() {
		long customerNo = requireCustomerNo();
		List<TvIndicatorTemplate> templates =
				tvIndicatorTemplateRepository.findByCustomerNoOrderByNameAsc(customerNo);
		return templates.stream().map(IndicatorTemplateServiceImpl::toListItem).toList();
	}

	private static IndicatorTemplateListItemDto toListItem(TvIndicatorTemplate template) {
		return new IndicatorTemplateListItemDto(template.getName());
	}

	private long requireCustomerNo() {
		Long customerNo = CustomerContext.get();
		if (customerNo == null) {
			throw new ServerErrorException();
		}

		return customerNo;
	}
}
