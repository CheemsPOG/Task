/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.entity.TvChartTemplate;
import com.task.chart.repository.TvChartTemplateRepository;
import com.task.chart.support.TestAuthSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * POST /api/chart-templates against design doc 137 (token, body, upsert).
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
class SystemOverviewDesign137Test {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TvChartTemplateRepository tvChartTemplateRepository;

	private String bearerDemo;
	private String bearerDemo2;

	@BeforeEach
	void authenticateDemoUsers() throws Exception {
		bearerDemo = TestAuthSupport.bearerDemo(mockMvc);
		bearerDemo2 = TestAuthSupport.bearerDemo2(mockMvc);
	}

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"name\":\"Dark\",\"content\":\"{}\"}"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class BodyValidation {

		@Test
		void blankNameReturns422() throws Exception {
			mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content("{\"name\":\"  \",\"content\":\"{}\"}"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void nameLongerThan64Returns422() throws Exception {
			String name = "A".repeat(65);
			mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content("{\"name\":\"" + name + "\",\"content\":\"{}\"}"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void blankContentReturns422() throws Exception {
			mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content("{\"name\":\"Dark\",\"content\":\"\"}"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class Upsert {

		@Test
		void firstPostPersistsRowAndReturnsUpdatedAt() throws Exception {
			long now = System.currentTimeMillis() / 1000L;
			MvcResult result = mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content("{\"name\":\"My Dark\",\"content\":\"{\\\"theme\\\":\\\"dark\\\"}\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.t").isNumber())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("t").asLong()).isBetween(now - 5, now + 5);

			List<TvChartTemplate> rows = tvChartTemplateRepository.findByCustomerNoOrderByNameAsc(1L);
			assertThat(rows).anySatisfy(row -> {
				assertThat(row.getName()).isEqualTo("My Dark");
				assertThat(row.getContent()).isEqualTo("{\"theme\":\"dark\"}");
				assertThat(row.getCustomerNo()).isEqualTo(1L);
			});
		}

		@Test
		void secondPostSameNameUpdatesContentOnly() throws Exception {
			mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content("{\"name\":\"KeepName\",\"content\":\"{\\\"v\\\":1}\"}"))
					.andExpect(status().isOk());

			TvChartTemplate first = tvChartTemplateRepository.findByCustomerNoAndName(1L, "KeepName").orElseThrow();
			long firstId = first.getId();
			long firstTs = first.getUpdatedAt().getEpochSecond();

			Thread.sleep(1100);

			mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content("{\"name\":\"KeepName\",\"content\":\"{\\\"v\\\":2}\"}"))
					.andExpect(status().isOk());

			TvChartTemplate updated = tvChartTemplateRepository.findByCustomerNoAndName(1L, "KeepName").orElseThrow();
			assertThat(updated.getId()).isEqualTo(firstId);
			assertThat(updated.getName()).isEqualTo("KeepName");
			assertThat(updated.getCustomerNo()).isEqualTo(1L);
			assertThat(updated.getContent()).isEqualTo("{\"v\":2}");
			assertThat(updated.getUpdatedAt().getEpochSecond()).isGreaterThanOrEqualTo(firstTs);

			MvcResult listResult = mockMvc.perform(get("/api/chart-templates")
							.header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();
			JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString());
			int named = 0;
			for (JsonNode item : list) {
				if ("KeepName".equals(item.get("name").asText())) {
					named++;
					assertThat(item.has("content")).isFalse();
				}
			}
			assertThat(named).isEqualTo(1);
		}

		@Test
		void otherCustomerCanReuseSameName() throws Exception {
			mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content("{\"name\":\"Shared\",\"content\":\"{\\\"c\\\":1}\"}"))
					.andExpect(status().isOk());

			mockMvc.perform(post("/api/chart-templates")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo2)
							.content("{\"name\":\"Shared\",\"content\":\"{\\\"c\\\":2}\"}"))
					.andExpect(status().isOk());

			TvChartTemplate demo = tvChartTemplateRepository.findByCustomerNoAndName(1L, "Shared").orElseThrow();
			TvChartTemplate demo2 = tvChartTemplateRepository.findByCustomerNoAndName(2L, "Shared").orElseThrow();
			assertThat(demo.getId()).isNotEqualTo(demo2.getId());
			assertThat(demo.getContent()).isEqualTo("{\"c\":1}");
			assertThat(demo2.getContent()).isEqualTo("{\"c\":2}");
		}
	}
}
