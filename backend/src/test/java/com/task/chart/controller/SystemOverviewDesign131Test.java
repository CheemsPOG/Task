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
 * DELETE /api/layouts/{id} against design doc 131 (token, path id, delete, system time).
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/21</td><td>Task</td><td>新規作成</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SystemOverviewDesign131Test {

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

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			long id = seedLayout("1");
			mockMvc.perform(delete("/api/layouts/" + id)).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class PathParameterCheck {

		@Test
		void nonNumericIdReturns422() throws Exception {
			mockMvc.perform(delete("/api/layouts/abc").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class DeleteLayout {

		@Test
		void unknownIdReturns404() throws Exception {
			mockMvc.perform(delete("/api/layouts/999999").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void otherCustomerReturns404AndRowRemains() throws Exception {
			long id = seedLayout("1");
			mockMvc.perform(delete("/api/layouts/" + id).header(HttpHeaders.AUTHORIZATION, bearerDemo2))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));

			mockMvc.perform(get("/api/layouts/" + id).header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(id));
		}

		@Test
		void happyPathDeletesAndReturnsSystemDatetime() throws Exception {
			long id = seedLayout("1");
			long now = System.currentTimeMillis() / 1000L;

			MvcResult result = mockMvc.perform(delete("/api/layouts/" + id).header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.t").isNumber())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("t").asLong()).isBetween(now - 5, now + 5);

			mockMvc.perform(get("/api/layouts/" + id).header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));

			MvcResult listResult = mockMvc.perform(get("/api/layouts").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();
			JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString());
			assertThat(list.isArray()).isTrue();
			for (JsonNode item : list) {
				assertThat(item.get("id").asLong()).isNotEqualTo(id);
			}
		}
	}

	private long seedLayout(String customerNo) throws Exception {
		String body = """
				{"name":"DeleteMe","content":"{\\"pane\\":1}","symbol":"USDJPY","resolution":"1D"}
				""";
		MvcResult result = mockMvc.perform(post("/api/layouts")
						.header(HttpHeaders.AUTHORIZATION, "1".equals(customerNo) ? bearerDemo : bearerDemo2)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}
}
