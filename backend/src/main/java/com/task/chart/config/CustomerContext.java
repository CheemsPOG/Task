/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.config;

/**
 * Holds the customer number for the current request (from JWT claim).
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
public final class CustomerContext {

	private static final ThreadLocal<Long> CUSTOMER_NO = new ThreadLocal<>();

	private CustomerContext() {
	}

	public static void set(long customerNo) {
		CUSTOMER_NO.set(customerNo);
	}

	public static Long get() {
		return CUSTOMER_NO.get();
	}

	public static void clear() {
		CUSTOMER_NO.remove();
	}
}
