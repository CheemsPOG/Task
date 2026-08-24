/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.repository.TvChartTemplateRepository;
import com.task.chart.support.TestAuthSupport;
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
 * DELETE /api/chart-templates/{name} against design doc 139 (token, name, hard delete).
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
class SystemOverviewDesign139Test {

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
			upsert("ToDelete", "{}");
			mockMvc.perform(delete("/api/chart-templates/ToDelete")).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class NameValidation {

		@Test
		void nameLongerThan64Returns422() throws Exception {
			String name = "A".repeat(65);
			mockMvc.perform(delete("/api/chart-templates/" + name).header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class DeleteTemplate {

		@Test
		void unknownNameReturns404() throws Exception {
			mockMvc.perform(delete("/api/chart-templates/Missing").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void otherCustomerReturns404AndRowRemains() throws Exception {
			upsert("KeepMine", "{\"k\":1}");
			mockMvc.perform(delete("/api/chart-templates/KeepMine").header(HttpHeaders.AUTHORIZATION, bearerDemo2))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));

			assertThat(tvChartTemplateRepository.findByCustomerNoAndName(1L, "KeepMine")).isPresent();
			mockMvc.perform(get("/api/chart-templates/KeepMine").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.name").value("KeepMine"));
		}

		@Test
		void happyPathDeletesAndReturnsSystemDatetime() throws Exception {
			upsert("Gone", "{\"g\":1}");
			long now = System.currentTimeMillis() / 1000L;

			MvcResult result = mockMvc.perform(delete("/api/chart-templates/Gone")
							.header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.t").isNumber())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("t").asLong()).isBetween(now - 5, now + 5);

			mockMvc.perform(get("/api/chart-templates/Gone").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));

			assertThat(tvChartTemplateRepository.findByCustomerNoAndName(1L, "Gone")).isEmpty();

			MvcResult listResult = mockMvc.perform(get("/api/chart-templates")
							.header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();
			JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString());
			for (JsonNode item : list) {
				assertThat(item.get("name").asText()).isNotEqualTo("Gone");
			}
		}
	}

	private void upsert(String name, String content) throws Exception {
		mockMvc.perform(post("/api/chart-templates")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, bearerDemo)
						.content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
								.put("name", name)
								.put("content", content))))
				.andExpect(status().isOk());
	}
}
