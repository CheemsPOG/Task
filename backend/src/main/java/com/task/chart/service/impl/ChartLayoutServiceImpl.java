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
 * is the HTTP caller. Another tenant's id looks like 404, not 403, so the API never confirms
 * the id exists. Missing {@link CustomerContext} is 500 ({@code ServerErrorException}), not 401 —
 * the filter should have set it; unset context is an internal bug.
 *
 * <p><strong>NOT:</strong> not chart templates (136–139), not indicator templates (132–135),
 * not Peach S-01, not the widget. Template services must copy this 404-vs-403 and 500-vs-401
 * pattern — do not filter tenant from {@code SecurityContextHolder} instead.
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
 *   <tr><td>1.3.2</td><td>2026/08/31</td><td>Task</td><td>Review comments on tenant 404/500</td></tr>
 *   <tr><td>1.3.3</td><td>2026/08/31</td><td>Task</td><td>Method overview Javadocs on helpers</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.3.3
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

	/**
	 * Doc 127. Inserts for the JWT tenant. Unknown/deleted pair is 404 (catalog miss),
	 * not 422 — contrast history, which 422s an unknown CD.
	 */
	@Override
	@Transactional
	public ChartLayoutIdResponse register(RegisterChartLayoutRequest request) {
		if (request == null) {
			throw new ValidationException();
		}

		validateUpsertBody(request);
		String ccypairCd = normalizeCcypairCd(request.symbol());
		requireActivePair(ccypairCd);

		long customerNo = CustomerContext.requireCustomerNo();
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

	/**
	 * Doc 128. Other tenant's id is 404 via {@link #requireOwnedLayout}. Content, name,
	 * symbol, and resolution all replace — this is not a partial PATCH.
	 */
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

	/**
	 * Doc 129. Same 404-not-403 as update/delete so the caller cannot probe foreign ids.
	 */
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

	/**
	 * Doc 130. Only this tenant's rows. Missing {@link CustomerContext} is 500, not 401.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ChartLayoutListItemDto> list() {
		long customerNo = CustomerContext.requireCustomerNo();
		List<TvChartLayout> layouts = tvChartLayoutRepository.findByCustomerNoOrderByUpdatedAtDesc(customerNo);
		return layouts.stream().map(ChartLayoutServiceImpl::toListItem).toList();
	}

	/**
	 * Doc 131. Deletes only after the tenant check. Response {@code t} is server-now seconds.
	 */
	@Override
	@Transactional
	public SystemDatetimeResponse delete(String idPath) {
		TvChartLayout layout = requireOwnedLayout(idPath);
		tvChartLayoutRepository.delete(layout);
		return new SystemDatetimeResponse(Instant.now().getEpochSecond());
	}

	/**
	 * List row: JSON {@code resolution} is DB {@code chart_type} ({@code 1D}/{@code 60},
	 * not {@code DAY}/{@code 60M}). Content blob is omitted on the list endpoint.
	 */
	private static ChartLayoutListItemDto toListItem(TvChartLayout layout) {
		return new ChartLayoutListItemDto(
				layout.getId(),
				layout.getName(),
				layout.getChartType(),
				layout.getCcypairCd(),
				layout.getUpdatedAt().getEpochSecond());
	}

	/**
	 * Inherited {@code findById} is unscoped. Compare {@code customer_no} here so another
	 * tenant's row is 404, not 403 (never confirm the id exists).
	 */
	private TvChartLayout requireOwnedLayout(String idPath) {
		long layoutId = parseLayoutId(idPath);
		long customerNo = CustomerContext.requireCustomerNo();
		TvChartLayout layout = tvChartLayoutRepository.findById(layoutId).orElse(null);

		// Other tenants' layouts look missing so we do not leak ids.
		if (layout == null || layout.getCustomerNo() != customerNo) {
			throw new ResourceNotFoundException();
		}

		return layout;
	}

	/**
	 * Non-numeric id is 422, not 404 — malformed path is validation, not "missing resource."
	 */
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

	/**
	 * Soft-deleted or unknown CD is 404 (catalog), not 422. History uses 422 for the
	 * same kind of miss — keep the two APIs different.
	 */
	private void requireActivePair(String ccypairCd) {
		Optional<Ccypair> found = ccypairRepository.findByCcypairCdAndIsDeleted(ccypairCd, Ccypair.ACTIVE);
		if (found.isEmpty()) {
			throw new ResourceNotFoundException();
		}
	}

	/**
	 * Layout resolution uses {@link ResolutionMapper#isMarksResolution} (no {@code 10}),
	 * not the history list. Blank/oversize name is 422 with no custom message.
	 */
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

	/**
	 * Same as resolve: strip {@code FX:} then slashes so {@code FX:USD/JPY} → {@code USDJPY}.
	 * Not the history letter-only strip ({@code ChartDataServiceImpl#normalizeSymbolCd}).
	 */
	private static String normalizeCcypairCd(String symbolName) {
		String upper = symbolName.trim().toUpperCase(Locale.ROOT);

		// Widget may send FX:USD/JPY; warehouse keys use USDJPY.
		if (upper.startsWith("FX:")) {
			upper = upper.substring(3);
		}

		return upper.replace("/", "");
	}
}
