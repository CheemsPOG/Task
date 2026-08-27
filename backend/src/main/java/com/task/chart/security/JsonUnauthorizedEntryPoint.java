/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Returns JSON 401 for missing/invalid Bearer tokens.
 *
 * <p>Spring Security {@code AuthenticationEntryPoint} wired in {@link SecurityConfig}. Writes
 * {@code E_UNAUTHORIZED} via {@code messages.properties} / {@code messages_ja.properties} when a
 * protected matcher has no valid access JWT. Filter-chain 401 is this class;
 * {@link com.task.chart.exception.GlobalExceptionHandler} handles thrown
 * {@link com.task.chart.exception.UnauthorizedAppException} from refresh. This is NOT Peach S-01
 * and NOT the widget.
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
@Component
public class JsonUnauthorizedEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;

	/**
	 * Creates the entry point.
	 *
	 * @param messageSource message bundle
	 * @param localeResolver request locale (Accept-Language)
	 */
	public JsonUnauthorizedEntryPoint(MessageSource messageSource, LocaleResolver localeResolver) {
		this.messageSource = messageSource;
		this.localeResolver = localeResolver;
	}

	/**
	 * Writes {@code E_UNAUTHORIZED} JSON when a protected matcher has no valid access JWT.
	 *
	 * @param request HTTP request
	 * @param response HTTP response
	 * @param authException Spring Security rejection
	 */
	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		Locale locale = localeResolver.resolveLocale(request);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		String message = messageSource.getMessage(ErrorCodes.MSG_UNAUTHORIZED, null, locale);
		ErrorResponse body = ErrorResponse.of(ErrorCodes.UNAUTHORIZED, message);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
