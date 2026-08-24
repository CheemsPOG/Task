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
 * GET /api/indicator-templates/{name} against design doc 134 (token, name, DTO).
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
class SystemOverviewDesign134Test {

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
			mockMvc.perform(get("/api/indicator-templates/RSI")).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class NameValidation {

		@Test
		void nameLongerThan64Returns422() throws Exception {
			String name = "A".repeat(65);
			mockMvc.perform(get("/api/indicator-templates/" + name).header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class Retrieval {

		@Test
		void unknownNameReturns404() throws Exception {
			mockMvc.perform(get("/api/indicator-templates/Missing").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void otherCustomerReturns404() throws Exception {
			upsert("TenantOnly", "{\"a\":1}", bearerDemo);
			mockMvc.perform(get("/api/indicator-templates/TenantOnly").header(HttpHeaders.AUTHORIZATION, bearerDemo2))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void happyPathReturnsNameAndContent() throws Exception {
			upsert("My RSI", "{\"studies\":[]}", bearerDemo);
			MvcResult result = mockMvc.perform(get("/api/indicator-templates/{name}", "My RSI")
							.header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.name").value("My RSI"))
					.andExpect(jsonPath("$.content").value("{\"studies\":[]}"))
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.has("customer_no")).isFalse();
			assertThat(root.has("timestamp")).isFalse();
		}

		@Test
		void getShowsContentAfterUpsert() throws Exception {
			upsert("RoundTrip", "{\"v\":1}", bearerDemo);
			mockMvc.perform(get("/api/indicator-templates/RoundTrip").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content").value("{\"v\":1}"));

			upsert("RoundTrip", "{\"v\":9}", bearerDemo);
			mockMvc.perform(get("/api/indicator-templates/RoundTrip").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.name").value("RoundTrip"))
					.andExpect(jsonPath("$.content").value("{\"v\":9}"));
		}
	}

	private void upsert(String name, String content, String bearer) throws Exception {
		mockMvc.perform(post("/api/indicator-templates")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, bearer)
						.content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
								.put("name", name)
								.put("content", content))))
				.andExpect(status().isOk());
	}
}
