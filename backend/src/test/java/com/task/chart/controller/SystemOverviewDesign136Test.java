/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.entity.TvChartTemplate;
import com.task.chart.repository.TvChartTemplateRepository;
import com.task.chart.security.JwtService;
import com.task.chart.support.TestAuthSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /api/chart-templates against design doc 136 (token, customer filter, name-only list).
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
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SystemOverviewDesign136Test {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	private String bearerDemo;
	private String bearerDemo2;

	@BeforeEach
	void authenticateDemoUsers() throws Exception {
		bearerDemo = TestAuthSupport.bearerDemo(mockMvc);
		bearerDemo2 = TestAuthSupport.bearerDemo2(mockMvc);
	}

	@Autowired
	private TvChartTemplateRepository tvChartTemplateRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/chart-templates")).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class ListRetrieval {

		@Test
		void emptyListWhenCustomerHasNoTemplates() throws Exception {
			MvcResult result = mockMvc.perform(get("/api/chart-templates")
							.header(HttpHeaders.AUTHORIZATION, TestAuthSupport.bearerForCustomer(jwtService, "ghost99", 99L)))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.isArray()).isTrue();
			assertThat(root).isEmpty();
		}

		@Test
		void returnsOnlyCurrentCustomerNamesSortedAsc() throws Exception {
			seed(1L, "Zulu", "{\"a\":1}");
			seed(1L, "Alpha", "{\"b\":2}");
			seed(2L, "Other", "{\"c\":3}");

			MvcResult result = mockMvc.perform(get("/api/chart-templates").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.isArray()).isTrue();

			List<String> names = new ArrayList<>();
			for (JsonNode item : root) {
				assertThat(item.has("name")).isTrue();
				assertThat(item.has("content")).isFalse();
				assertThat(item.has("customer_no")).isFalse();
				names.add(item.get("name").asText());
			}

			assertThat(names).contains("Alpha", "Zulu");
			assertThat(names).doesNotContain("Other");
			assertThat(names).isSorted();
		}
	}

	private void seed(long customerNo, String name, String content) {
		tvChartTemplateRepository.save(new TvChartTemplate(customerNo, name, content, Instant.now()));
	}
}
