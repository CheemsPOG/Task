/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.support.TestAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Doc 124 symbol is ccypair_cd</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SymbolSearchFilterTest {

	@Autowired
	private MockMvc mockMvc;

	private String bearerDemo;
	private String bearerDemo2;

	@BeforeEach
	void authenticateDemoUsers() throws Exception {
		bearerDemo = TestAuthSupport.bearerDemo(mockMvc);
		bearerDemo2 = TestAuthSupport.bearerDemo2(mockMvc);
	}

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void returnsFivePairsWhenWidgetFiltersByCtfxAndForex() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/search")
						.param("query", "")
						.param("exchange", "CTFX")
						.param("type", "FOREX")
						.param("limit", "50")
						.header(HttpHeaders.AUTHORIZATION, bearerDemo))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(root.isArray()).isTrue();
		assertThat(root).hasSize(5);
		assertThat(root.get(0).get("exchange").asText()).isEqualTo("CTFX");
		assertThat(root.get(0).get("type").asText()).isEqualTo("FOREX");
		assertThat(root.get(0).get("symbol").asText()).isEqualTo("USDJPY");
		assertThat(root.get(0).get("ticker").asText()).isEqualTo("USD/JPY");
		assertThat(root.get(0).get("description").asText()).isEqualTo("米ドル/円");
	}

	@Test
	void usdQueryStillFindsUsdJpy() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/search")
						.param("query", "USD")
						.param("exchange", "CTFX")
						.param("type", "FOREX")
						.header(HttpHeaders.AUTHORIZATION, bearerDemo))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(root.size()).isGreaterThanOrEqualTo(1);
		boolean hasUsdJpy = false;
		for (JsonNode node : root) {
			if ("USDJPY".equals(node.get("symbol").asText())) {
				hasUsdJpy = true;
				break;
			}
		}
		assertThat(hasUsdJpy).isTrue();
	}
}
