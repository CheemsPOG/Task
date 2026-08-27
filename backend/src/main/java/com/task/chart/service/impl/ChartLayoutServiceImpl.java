/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.dto.request.RegisterChartLayoutRequest;
import com.task.chart.dto.response.ChartLayoutDto;
import com.task.chart.dto.response.ChartLayoutIdResponse;
import com.task.chart.dto.response.ChartLayoutListItemDto;
import com.task.chart.dto.response.SystemDatetimeResponse;
import com.task.chart.entity.Ccypair;
import com.task.chart.entity.TvChartLayout;
import com.task.chart.exception.ResourceNotFoundException;
import com.task.chart.exception.ServerErrorException;
import com.task.chart.exception.ValidationException;
import com.task.chart.repository.CcypairRepository;
import com.task.chart.repository.TvChartLayoutRepository;
import com.task.chart.security.CustomerContext;
import com.task.chart.service.ChartLayoutService;
import com.task.chart.util.ResolutionMapper;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link ChartLayoutService}.
 *
 * <p>Docs 127–131 on table {@code m_tv_chart_layout}, scoped by JWT {@code customer_no}. Symbol
 * must be an active {@code m_ccypairs} CD. {@link com.task.chart.controller.ChartLayoutController}
 * is the HTTP caller. Another tenant's id looks like 404, not 403. This is NOT chart templates,
 * NOT indicator templates, NOT Peach S-01, and NOT the widget.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Add get for doc 129</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/21</td><td>Task</td><td>Add list for doc 130</td></tr>
 *   <tr><td>1.3.0</td><td>2026/08/21</td><td>Task</td><td>Add delete for doc 131</td></tr>
 *   <tr><td>1.3.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.3.1
 */
@Service
public class ChartLayoutServiceImpl implements ChartLayoutService {

	private static final int NAME_MAX_LENGTH = 64;

	private final TvChartLayoutRepository tvChartLayoutRepository;
	private final CcypairRepository ccypairRepository;

	/**
	 * Creates the service.
	 *
	 * @param tvChartLayoutRepository layout table
	 * @param ccypairRepository currency pair master
	 */
	public ChartLayoutServiceImpl(
			TvChartLayoutRepository tvChartLayoutRepository,
			CcypairRepository ccypairRepository) {
		this.tvChartLayoutRepository = tvChartLayoutRepository;
		this.ccypairRepository = ccypairRepository;
	}

	@Override
	@Transactional
	public ChartLayoutIdResponse register(RegisterChartLayoutRequest request) {
		if (request == null) {
			throw new ValidationException();
		}

		validateUpsertBody(request);
		String ccypairCd = normalizeCcypairCd(request.symbol());
		requireActivePair(ccypairCd);

		long customerNo = requireCustomerNo();
		TvChartLayout layout = new TvChartLayout(
				customerNo,
				request.name().trim(),
				request.content(),
				ccypairCd,
				request.resolution(),
				Instant.now());
		TvChartLayout saved = tvChartLayoutRepository.save(layout);
		return new ChartLayoutIdResponse(saved.getId());
	}

	@Override
	@Transactional
	public ChartLayoutIdResponse update(String idPath, RegisterChartLayoutRequest request) {
		if (request == null) {
			throw new ValidationException();
		}

		validateUpsertBody(request);
		String ccypairCd = normalizeCcypairCd(request.symbol());
		requireActivePair(ccypairCd);

		TvChartLayout layout = requireOwnedLayout(idPath);
		layout.applyUpdate(
				request.name().trim(),
				request.content(),
				ccypairCd,
				request.resolution(),
				Instant.now());
		tvChartLayoutRepository.save(layout);
		return new ChartLayoutIdResponse(layout.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public ChartLayoutDto get(String idPath) {
		TvChartLayout layout = requireOwnedLayout(idPath);
		return new ChartLayoutDto(
				layout.getId(),
				layout.getName(),
				layout.getUpdatedAt().getEpochSecond(),
				layout.getContent());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChartLayoutListItemDto> list() {
		long customerNo = requireCustomerNo();
		List<TvChartLayout> layouts = tvChartLayoutRepository.findByCustomerNoOrderByUpdatedAtDesc(customerNo);
		return layouts.stream().map(ChartLayoutServiceImpl::toListItem).toList();
	}

	@Override
	@Transactional
	public SystemDatetimeResponse delete(String idPath) {
		TvChartLayout layout = requireOwnedLayout(idPath);
		tvChartLayoutRepository.delete(layout);
		return new SystemDatetimeResponse(Instant.now().getEpochSecond());
	}

	private static ChartLayoutListItemDto toListItem(TvChartLayout layout) {
		return new ChartLayoutListItemDto(
				layout.getId(),
				layout.getName(),
				layout.getChartType(),
				layout.getCcypairCd(),
				layout.getUpdatedAt().getEpochSecond());
	}

	private TvChartLayout requireOwnedLayout(String idPath) {
		long layoutId = parseLayoutId(idPath);
		long customerNo = requireCustomerNo();
		TvChartLayout layout = tvChartLayoutRepository.findById(layoutId).orElse(null);

		// Other tenants' layouts look missing so we do not leak ids.
		if (layout == null || layout.getCustomerNo() != customerNo) {
			throw new ResourceNotFoundException();
		}

		return layout;
	}

	private long requireCustomerNo() {
		Long customerNo = CustomerContext.get();

		// Filter should have set tenant; missing context is a server bug, not 401.
		if (customerNo == null) {
			throw new ServerErrorException();
		}

		return customerNo;
	}

	private static long parseLayoutId(String idPath) {
		if (idPath == null || idPath.isBlank()) {
			throw new ValidationException();
		}

		try {
			return Long.parseLong(idPath.trim());
		} catch (NumberFormatException ex) {
			throw new ValidationException();
		}
	}

	private void requireActivePair(String ccypairCd) {
		Optional<Ccypair> found = ccypairRepository.findByCcypairCdAndIsDeleted(ccypairCd, Ccypair.ACTIVE);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException();
		}
	}

	private static void validateUpsertBody(RegisterChartLayoutRequest request) {
		String name = request.name();
		if (name == null || name.isBlank() || name.trim().length() > NAME_MAX_LENGTH) {
			throw new ValidationException();
		}

		String content = request.content();
		if (content == null || content.isBlank()) {
			throw new ValidationException();
		}

		String symbol = request.symbol();
		if (symbol == null || symbol.isBlank()) {
			throw new ValidationException();
		}

		String resolution = request.resolution();
		if (resolution == null || resolution.isBlank() || !ResolutionMapper.isMarksResolution(resolution)) {
			throw new ValidationException();
		}
	}

	private static String normalizeCcypairCd(String symbolName) {
		String upper = symbolName.trim().toUpperCase(Locale.ROOT);

		// Widget may send FX:USD/JPY; warehouse keys use USDJPY.
		if (upper.startsWith("FX:")) {
			upper = upper.substring(3);
		}

		return upper.replace("/", "");
	}
}
