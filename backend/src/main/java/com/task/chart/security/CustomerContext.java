/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

/**
 * Holds the customer number for the current request (from JWT claim).
 *
 * <p>Request-scoped {@code ThreadLocal}: {@link JwtAuthenticationFilter} sets it from the access JWT
 * and always {@link #clear()}s in {@code finally}. Layout and template services read {@link #get()}
 * to scope {@code m_tv_chart_layout}, {@code m_tv_indicator_template}, and {@code m_tv_chart_templates}
 * by tenant. This is NOT an HTTP session, NOT Peach S-01, and NOT shared with the Python WS.
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Move to security package</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
public final class CustomerContext {

	private static final ThreadLocal<Long> CUSTOMER_NO = new ThreadLocal<>();

	private CustomerContext() {
	}

	/**
	 * Stores the token customer for this thread.
	 *
	 * @param customerNo JWT {@code customer_no} claim
	 */
	public static void set(long customerNo) {
		CUSTOMER_NO.set(customerNo);
	}

	/**
	 * @return token customer, or {@code null} if unauthenticated
	 */
	public static Long get() {
		return CUSTOMER_NO.get();
	}

	/**
	 * Clears the thread-local so it cannot leak to the next request.
	 */
	public static void clear() {
		CUSTOMER_NO.remove();
	}
}
