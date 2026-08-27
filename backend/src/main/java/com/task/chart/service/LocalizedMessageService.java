/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.service;

/**
 * Resolves localized messages from the message bundle.
 *
 * <p>Looks up {@code error.*} keys in {@code messages.properties} / {@code messages_ja.properties}
 * using {@code Accept-Language}. {@link com.task.chart.exception.GlobalExceptionHandler} is the main
 * caller. Implemented by {@link com.task.chart.service.impl.LocalizedMessageServiceImpl}. This is NOT
 * Peach i18n services, NOT the Python WS, and NOT the widget copy.
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
public interface LocalizedMessageService {

	/**
	 * Resolves a message key for the current locale ({@code Accept-Language}).
	 *
	 * @param code message key
	 * @return localized text
	 */
	String get(String code);
}
