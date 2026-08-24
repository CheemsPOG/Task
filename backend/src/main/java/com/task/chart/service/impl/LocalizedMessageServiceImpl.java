/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service.impl;

import com.task.chart.service.LocalizedMessageService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * MessageSource-backed {@link LocalizedMessageService}.
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
public class LocalizedMessageServiceImpl implements LocalizedMessageService {

	private final MessageSource messageSource;

	/**
	 * Creates the service.
	 *
	 * @param messageSource Spring message source
	 */
	public LocalizedMessageServiceImpl(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * @return localized text for the message key
	 */
	@Override
	public String get(String code) {
		return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
	}
}
