/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * GET /api/layouts/{id} against design doc 129 (token, path id, DTO, round-trip).
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
class SystemOverviewDesign129Test {

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
			long id = seedLayout();
			mockMvc.perform(get("/api/layouts/" + id)).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class PathParameterCheck {

		@Test
		void nonNumericIdReturns422() throws Exception {
			mockMvc.perform(get("/api/layouts/abc").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class Retrieval {

		@Test
		void unknownIdReturns404() throws Exception {
			mockMvc.perform(get("/api/layouts/999999").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void otherCustomerReturns404() throws Exception {
			long id = seedLayout();
			mockMvc.perform(get("/api/layouts/" + id).header(HttpHeaders.AUTHORIZATION, bearerDemo2))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void happyPathReturnsDtoAfterRegister() throws Exception {
			long id = seedLayout();
			MvcResult result = mockMvc.perform(get("/api/layouts/" + id).header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(id))
					.andExpect(jsonPath("$.name").value("Seed"))
					.andExpect(jsonPath("$.content").value("{\"pane\":1}"))
					.andExpect(jsonPath("$.timestamp").isNumber())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			long timestamp = root.get("timestamp").asLong();
			long now = System.currentTimeMillis() / 1000L;
			assertThat(timestamp).isBetween(now - 60, now + 5);
			assertThat(root.has("symbol")).isFalse();
			assertThat(root.has("resolution")).isFalse();
			assertThat(root.has("customer_no")).isFalse();
		}

		@Test
		void registerUpdateGetRoundTrip() throws Exception {
			long id = seedLayout();

			MvcResult before = mockMvc.perform(get("/api/layouts/" + id).header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();
			long tsBefore = objectMapper.readTree(before.getResponse().getContentAsString())
					.get("timestamp")
					.asLong();

			Thread.sleep(20);

			mockMvc.perform(put("/api/layouts/" + id)
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
									.put("name", "Renamed")
									.put("content", "{\"pane\":2}")
									.put("symbol", "EURUSD")
									.put("resolution", "60"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(id));

			MvcResult after = mockMvc.perform(get("/api/layouts/" + id).header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(id))
					.andExpect(jsonPath("$.name").value("Renamed"))
					.andExpect(jsonPath("$.content").value("{\"pane\":2}"))
					.andReturn();

			long tsAfter = objectMapper.readTree(after.getResponse().getContentAsString())
					.get("timestamp")
					.asLong();
			assertThat(tsAfter).isGreaterThanOrEqualTo(tsBefore);
		}
	}

	private long seedLayout() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/layouts")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, bearerDemo)
						.content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
								.put("name", "Seed")
								.put("content", "{\"pane\":1}")
								.put("symbol", "USDJPY")
								.put("resolution", "1D"))))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}
}
