/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ApiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /api/search must return demo pairs when the widget filters by CTFX/FOREX.
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
@SpringBootTest
@AutoConfigureMockMvc
class SymbolSearchFilterTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void returnsFivePairsWhenWidgetFiltersByCtfxAndForex() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/search")
						.param("query", "")
						.param("exchange", "CTFX")
						.param("type", "FOREX")
						.param("limit", "50")
						.header(ApiHeaders.CUSTOMER_NO, "1"))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(root.isArray()).isTrue();
		assertThat(root).hasSize(5);
		assertThat(root.get(0).get("exchange").asText()).isEqualTo("CTFX");
		assertThat(root.get(0).get("type").asText()).isEqualTo("FOREX");
		assertThat(root.get(0).get("symbol").asText()).isEqualTo("USD/JPY");
	}

	@Test
	void usdQueryStillFindsUsdJpy() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/search")
						.param("query", "USD")
						.param("exchange", "CTFX")
						.param("type", "FOREX")
						.header(ApiHeaders.CUSTOMER_NO, "1"))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(root.size()).isGreaterThanOrEqualTo(1);
		boolean hasUsdJpy = false;
		for (JsonNode node : root) {
			if ("USD/JPY".equals(node.get("symbol").asText())) {
				hasUsdJpy = true;
				break;
			}
		}
		assertThat(hasUsdJpy).isTrue();
	}
}
