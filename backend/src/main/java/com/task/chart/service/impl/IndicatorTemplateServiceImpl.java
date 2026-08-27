/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.dto.request.UpsertIndicatorTemplateRequest;
import com.task.chart.dto.response.IndicatorTemplateDto;
import com.task.chart.dto.response.IndicatorTemplateListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import com.task.chart.entity.TvIndicatorTemplate;
import com.task.chart.exception.ResourceNotFoundException;
import com.task.chart.exception.ServerErrorException;
import com.task.chart.exception.ValidationException;
import com.task.chart.repository.TvIndicatorTemplateRepository;
import com.task.chart.security.CustomerContext;
import com.task.chart.service.IndicatorTemplateService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link IndicatorTemplateService}.
 *
 * <p>Docs 132–135 on table {@code m_tv_indicator_template}, scoped by JWT {@code customer_no}.
 * {@link com.task.chart.controller.IndicatorTemplateController} is the HTTP caller. Unique key is
 * {@code (customer_no, name)}. This is NOT chart templates (136–139), NOT layouts, NOT Peach S-01,
 * and NOT the widget localStorage studies.
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Add upsert/get/delete for 133–135</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
@Service
public class IndicatorTemplateServiceImpl implements IndicatorTemplateService {

	private static final int NAME_MAX_LENGTH = 64;

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

	@Override
	@Transactional
	public SystemDatetimeResponse upsert(UpsertIndicatorTemplateRequest request) {
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
		Optional<TvIndicatorTemplate> found =
				tvIndicatorTemplateRepository.findByCustomerNoAndName(customerNo, name);
		TvIndicatorTemplate saved;

		// Same name for this tenant updates content; otherwise insert.
		if (found.isPresent()) {
			TvIndicatorTemplate row = found.get();
			row.applyContent(content, now);
			saved = tvIndicatorTemplateRepository.save(row);
		} else {
			saved = tvIndicatorTemplateRepository.save(new TvIndicatorTemplate(customerNo, name, content, now));
		}

		return new SystemDatetimeResponse(saved.getUpdatedAt().getEpochSecond());
	}

	@Override
	@Transactional(readOnly = true)
	public IndicatorTemplateDto get(String name) {
		TvIndicatorTemplate template = requireOwnedTemplate(name);
		return new IndicatorTemplateDto(template.getName(), template.getContent());
	}

	@Override
	@Transactional
	public SystemDatetimeResponse delete(String name) {
		TvIndicatorTemplate template = requireOwnedTemplate(name);
		tvIndicatorTemplateRepository.delete(template);
		return new SystemDatetimeResponse(Instant.now().getEpochSecond());
	}

	private static IndicatorTemplateListItemDto toListItem(TvIndicatorTemplate template) {
		return new IndicatorTemplateListItemDto(template.getName());
	}

	private TvIndicatorTemplate requireOwnedTemplate(String name) {
		String templateName = requireTemplateName(name);
		long customerNo = requireCustomerNo();
		Optional<TvIndicatorTemplate> found =
				tvIndicatorTemplateRepository.findByCustomerNoAndName(customerNo, templateName);
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

		// Filter should have set tenant; missing context is a server bug, not 401.
		if (customerNo == null) {
			throw new ServerErrorException();
		}

		return customerNo;
	}
}
