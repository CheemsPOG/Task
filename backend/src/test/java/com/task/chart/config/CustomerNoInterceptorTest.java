/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.task.chart.constants.ApiHeaders;
import com.task.chart.controller.ChartDataController;
import com.task.chart.dto.response.DatafeedConfigResponse;
import com.task.chart.dto.response.DatafeedConfigResponse.SymbolTypeDto;
import com.task.chart.service.ChartDataService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Stub S-01: {@code /api/health} is open; other {@code /api} routes need {@code X-Customer-No}.
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
@WebMvcTest(ChartDataController.class)
@Import({ WebAuthConfig.class, CustomerNoInterceptor.class })
class CustomerNoInterceptorTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChartDataService chartDataService;

	@Test
	void healthIsOpenWithoutCustomerHeader() throws Exception {
		mockMvc.perform(get("/api/health")).andExpect(status().isOk());
	}

	@Test
	void configWithoutCustomerHeaderIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/config")).andExpect(status().isUnauthorized());
	}

	@Test
	void configWithInvalidCustomerHeaderIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/config").header(ApiHeaders.CUSTOMER_NO, "abc"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void configWithCustomerHeaderIsOk() throws Exception {
		when(chartDataService.config()).thenReturn(sampleConfig());

		mockMvc.perform(get("/api/config").header(ApiHeaders.CUSTOMER_NO, "1"))
				.andExpect(status().isOk());
	}

	@Test
	void corsPreflightDoesNotRequireCustomerHeader() throws Exception {
		MvcResult result = mockMvc.perform(options("/api/config")).andReturn();
		assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
	}

	private static DatafeedConfigResponse sampleConfig() {
		return new DatafeedConfigResponse(
				true,
				false,
				false,
				false,
				true,
				List.of("1D"),
				List.of(),
				List.of(new SymbolTypeDto("forex", "forex")));
	}
}
