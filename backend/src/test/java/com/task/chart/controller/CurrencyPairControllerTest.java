/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.task.chart.support.TestAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * GET /curpairs catalog used to map WebSocket {@code curpairCd} (string) to a pair.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/24</td><td>Task</td><td>新規作成</td></tr>
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Require JWT</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class CurrencyPairControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private String bearerDemo;

	@BeforeEach
	void authenticateDemoUser() throws Exception {
		bearerDemo = TestAuthSupport.bearerDemo(mockMvc);
	}

	@Test
	void rejectsRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/curpairs")).andExpect(status().isUnauthorized());
	}

	@Test
	void listsActivePairsFromMCcypairsWithToken() throws Exception {
		mockMvc.perform(get("/curpairs").with(TestAuthSupport.withBearer(bearerDemo)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].curpairCd").value(1))
				.andExpect(jsonPath("$[0].curpairName").value("USDJPY"))
				.andExpect(jsonPath("$[0].curpairDisplay").value("USD/JPY"))
				.andExpect(jsonPath("$[1].curpairCd").value(2))
				.andExpect(jsonPath("$[1].curpairName").value("EURJPY"))
				.andExpect(jsonPath("$[1].curpairDisplay").value("EUR/JPY"))
				.andExpect(jsonPath("$[4].curpairCd").value(5))
				.andExpect(jsonPath("$[4].curpairName").value("AUDUSD"))
				.andExpect(jsonPath("$[4].curpairDisplay").value("AUD/USD"))
				.andExpect(jsonPath("$.length()").value(5));
	}
}
