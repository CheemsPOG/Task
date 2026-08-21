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
import com.task.chart.security.JwtService;
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
 * GET /api/layouts against design doc 130 (token, customer filter, sort, list DTO).
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
class SystemOverviewDesign130Test {

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

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/layouts")).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class ListRetrieval {

		@Test
		void emptyListWhenCustomerHasNoLayouts() throws Exception {
			MvcResult result = mockMvc.perform(get("/api/layouts").header(HttpHeaders.AUTHORIZATION, TestAuthSupport.bearerForCustomer(jwtService, "ghost99", 99L)))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.isArray()).isTrue();
			assertThat(root).isEmpty();
		}

		@Test
		void returnsOnlyCurrentCustomerLayouts() throws Exception {
			long id1 = registerAs("1", "Mine", "USDJPY", "1D");
			registerAs("2", "Theirs", "EURUSD", "60");

			MvcResult result = mockMvc.perform(get("/api/layouts").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.isArray()).isTrue();
			assertThat(root).isNotEmpty();
			for (JsonNode item : root) {
				assertThat(item.get("id").asLong()).isNotEqualTo(0L);
				assertThat(item.has("name")).isTrue();
				assertThat(item.has("resolution")).isTrue();
				assertThat(item.has("symbol")).isTrue();
				assertThat(item.has("timestamp")).isTrue();
				assertThat(item.has("content")).isFalse();
			}

			boolean foundMine = false;
			for (JsonNode item : root) {
				if (item.get("id").asLong() == id1) {
					foundMine = true;
					assertThat(item.get("name").asText()).isEqualTo("Mine");
					assertThat(item.get("symbol").asText()).isEqualTo("USDJPY");
					assertThat(item.get("resolution").asText()).isEqualTo("1D");
				}
				assertThat(item.get("name").asText()).isNotEqualTo("Theirs");
			}
			assertThat(foundMine).isTrue();
		}

		@Test
		void sortsByUpdatedAtDescending() throws Exception {
			long olderId = registerAs("1", "Older", "USDJPY", "1D");
			Thread.sleep(30);
			long newerId = registerAs("1", "Newer", "EURUSD", "60");

			MvcResult result = mockMvc.perform(get("/api/layouts").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.size()).isGreaterThanOrEqualTo(2);

			int newerIndex = -1;
			int olderIndex = -1;
			for (int i = 0; i < root.size(); i++) {
				long id = root.get(i).get("id").asLong();
				if (id == newerId) {
					newerIndex = i;
				}
				if (id == olderId) {
					olderIndex = i;
				}
			}
			assertThat(newerIndex).isGreaterThanOrEqualTo(0);
			assertThat(olderIndex).isGreaterThanOrEqualTo(0);
			assertThat(newerIndex).isLessThan(olderIndex);

			long tsNewer = root.get(newerIndex).get("timestamp").asLong();
			long tsOlder = root.get(olderIndex).get("timestamp").asLong();
			assertThat(tsNewer).isGreaterThanOrEqualTo(tsOlder);
		}

		@Test
		void updatedLayoutMovesTowardFront() throws Exception {
			long firstId = registerAs("1", "First", "USDJPY", "1D");
			Thread.sleep(30);
			long secondId = registerAs("1", "Second", "EURUSD", "60");

			Thread.sleep(30);
			mockMvc.perform(put("/api/layouts/" + firstId)
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo)
							.content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
									.put("name", "FirstUpdated")
									.put("content", "{\"pane\":9}")
									.put("symbol", "GBPUSD")
									.put("resolution", "15"))))
					.andExpect(status().isOk());

			MvcResult result = mockMvc.perform(get("/api/layouts").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[0].id").value(firstId))
					.andExpect(jsonPath("$[0].name").value("FirstUpdated"))
					.andExpect(jsonPath("$[0].symbol").value("GBPUSD"))
					.andExpect(jsonPath("$[0].resolution").value("15"))
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get(0).get("id").asLong()).isEqualTo(firstId);
			boolean sawSecond = false;
			for (JsonNode item : root) {
				if (item.get("id").asLong() == secondId) {
					sawSecond = true;
				}
			}
			assertThat(sawSecond).isTrue();
		}
	}

	private long registerAs(String customerNo, String name, String symbol, String resolution) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/layouts")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "1".equals(customerNo) ? bearerDemo : bearerDemo2)
						.content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
								.put("name", name)
								.put("content", "{\"pane\":1}")
								.put("symbol", symbol)
								.put("resolution", resolution))))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}
}
