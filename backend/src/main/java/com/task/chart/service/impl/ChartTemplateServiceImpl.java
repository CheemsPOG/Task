/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.dto.request.UpsertChartTemplateRequest;
import com.task.chart.dto.response.ChartTemplateDto;
import com.task.chart.dto.response.ChartTemplateListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import com.task.chart.entity.TvChartTemplate;
import com.task.chart.exception.ResourceNotFoundException;
import com.task.chart.exception.ServerErrorException;
import com.task.chart.exception.ValidationException;
import com.task.chart.repository.TvChartTemplateRepository;
import com.task.chart.security.CustomerContext;
import com.task.chart.service.ChartTemplateService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link ChartTemplateService}.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/24</td><td>Task</td><td>新規作成</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@Service
public class ChartTemplateServiceImpl implements ChartTemplateService {

	private static final int NAME_MAX_LENGTH = 64;

	private final TvChartTemplateRepository tvChartTemplateRepository;

	/**
	 * Creates the service.
	 *
	 * @param tvChartTemplateRepository chart template table
	 */
	public ChartTemplateServiceImpl(TvChartTemplateRepository tvChartTemplateRepository) {
		this.tvChartTemplateRepository = tvChartTemplateRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChartTemplateListItemDto> list() {
		long customerNo = requireCustomerNo();
		List<TvChartTemplate> templates = tvChartTemplateRepository.findByCustomerNoOrderByNameAsc(customerNo);
		return templates.stream().map(ChartTemplateServiceImpl::toListItem).toList();
	}

	@Override
	@Transactional
	public SystemDatetimeResponse upsert(UpsertChartTemplateRequest request) {
		if (request == null) {
			throw new ValidationException();
		}

		String name = requireTemplateName(request.name());
		String content = request.content();
		if (content == null || content.isBlank()) {
			throw new ValidationException();
		}

		long customerNo = requireCustomerNo();
		Instant now = Instant.now();
		Optional<TvChartTemplate> found = tvChartTemplateRepository.findByCustomerNoAndName(customerNo, name);
		TvChartTemplate saved;
		if (found.isPresent()) {
			TvChartTemplate row = found.get();
			row.applyContent(content, now);
			saved = tvChartTemplateRepository.save(row);
		} else {
			saved = tvChartTemplateRepository.save(new TvChartTemplate(customerNo, name, content, now));
		}

		return new SystemDatetimeResponse(saved.getUpdatedAt().getEpochSecond());
	}

	@Override
	@Transactional(readOnly = true)
	public ChartTemplateDto get(String name) {
		TvChartTemplate template = requireOwnedTemplate(name);
		return new ChartTemplateDto(template.getName(), template.getContent());
	}

	@Override
	@Transactional
	public SystemDatetimeResponse delete(String name) {
		TvChartTemplate template = requireOwnedTemplate(name);
		tvChartTemplateRepository.delete(template);
		return new SystemDatetimeResponse(Instant.now().getEpochSecond());
	}

	private static ChartTemplateListItemDto toListItem(TvChartTemplate template) {
		return new ChartTemplateListItemDto(template.getName());
	}

	private TvChartTemplate requireOwnedTemplate(String name) {
		String templateName = requireTemplateName(name);
		long customerNo = requireCustomerNo();
		Optional<TvChartTemplate> found =
				tvChartTemplateRepository.findByCustomerNoAndName(customerNo, templateName);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException();
		}

		return found.get();
	}

	private static String requireTemplateName(String name) {
		if (name == null || name.isBlank()) {
			throw new ValidationException();
		}

		String trimmed = name.trim();
		if (trimmed.length() > NAME_MAX_LENGTH) {
			throw new ValidationException();
		}

		return trimmed;
	}

	private long requireCustomerNo() {
		Long customerNo = CustomerContext.get();
		if (customerNo == null) {
			throw new ServerErrorException();
		}

		return customerNo;
	}
}
