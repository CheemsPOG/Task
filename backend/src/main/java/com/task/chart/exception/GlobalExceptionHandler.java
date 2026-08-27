/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.exception;

import com.task.chart.constants.ErrorCodes;
import com.task.chart.dto.response.ErrorResponse;
import com.task.chart.service.LocalizedMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps exceptions to HTTP status and localized JSON error bodies.
 *
 * <p>REST {@code @RestControllerAdvice}: {@code CODE:30020} (422), {@code CODE:30404} (404),
 * {@code E_BAD_CREDENTIALS} / {@code E_UNAUTHORIZED} (401), {@code E_SERVER} (500). Messages come from
 * {@code messages.properties} via {@link com.task.chart.service.LocalizedMessageService}. Controllers
 * do not catch. Filter-chain 401 for missing Bearer is
 * {@link com.task.chart.security.JsonUnauthorizedEntryPoint}, not this class. This is NOT Peach
 * S-01 and NOT the Python WS.
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>MessageSource + auth errors</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/24</td><td>Task</td><td>@ResponseStatus for OpenAPI docs</td></tr>
 *   <tr><td>1.2.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.1
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private final LocalizedMessageService localizedMessageService;

	/**
	 * Creates the advice.
	 *
	 * @param localizedMessageService message bundle
	 */
	public GlobalExceptionHandler(LocalizedMessageService localizedMessageService) {
		this.localizedMessageService = localizedMessageService;
	}

	@ExceptionHandler(ValidationException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(ErrorResponse.of(
						ErrorCodes.VALIDATION,
						localizedMessageService.get(ErrorCodes.MSG_VALIDATION)));
	}

	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class
	})
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {

		// Malformed JSON / types use the same 422 code as business ValidationException.
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(ErrorResponse.of(
						ErrorCodes.VALIDATION,
						localizedMessageService.get(ErrorCodes.MSG_BAD_REQUEST)));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of(
						ErrorCodes.NOT_FOUND,
						localizedMessageService.get(ErrorCodes.MSG_NOT_FOUND)));
	}

	@ExceptionHandler(BadCredentialsAppException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsAppException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ErrorResponse.of(
						ErrorCodes.BAD_CREDENTIALS,
						localizedMessageService.get(ErrorCodes.MSG_BAD_CREDENTIALS)));
	}

	@ExceptionHandler(UnauthorizedAppException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedAppException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ErrorResponse.of(
						ErrorCodes.UNAUTHORIZED,
						localizedMessageService.get(ErrorCodes.MSG_UNAUTHORIZED)));
	}

	@ExceptionHandler(ServerErrorException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<ErrorResponse> handleServer(ServerErrorException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of(
						ErrorCodes.SERVER,
						localizedMessageService.get(ErrorCodes.MSG_SERVER)));
	}
}
