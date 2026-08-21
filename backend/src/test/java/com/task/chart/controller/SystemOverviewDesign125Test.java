/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.support.TestAuthSupport;
import com.task.chart.constants.MarkSeedWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * GET /api/marks against design doc 125 (token, validation, m_tv_mark).
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
class SystemOverviewDesign125Test {

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
			mockMvc.perform(seededMarks()).andExpect(status().isUnauthorized());
		}

		@Test
		void validTokenReturns200() throws Exception {
			mockMvc.perform(authorizedSeededMarks()).andExpect(status().isOk());
		}
	}

	@Nested
	class ValidationCheck {

		@Test
		void missingSymbolReturns422() throws Exception {
			mockMvc.perform(get("/api/marks")
							.param("resolution", "1D")
							.param("from", String.valueOf(MarkSeedWindow.FROM))
							.param("to", String.valueOf(MarkSeedWindow.TO))
							.header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void missingResolutionReturns422() throws Exception {
			mockMvc.perform(get("/api/marks")
							.param("symbol", "USDJPY")
							.param("from", String.valueOf(MarkSeedWindow.FROM))
							.param("to", String.valueOf(MarkSeedWindow.TO))
							.header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void missingFromReturns422() throws Exception {
			mockMvc.perform(get("/api/marks")
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("to", String.valueOf(MarkSeedWindow.TO))
							.header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void missingToReturns422() throws Exception {
			mockMvc.perform(get("/api/marks")
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(MarkSeedWindow.FROM))
							.header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void unsupportedResolutionReturns422() throws Exception {
			mockMvc.perform(authorizedMarks("USDJPY", "10", MarkSeedWindow.FROM, MarkSeedWindow.TO))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void toBeforeFromReturns422() throws Exception {
			mockMvc.perform(authorizedMarks("USDJPY", "1D", MarkSeedWindow.TO, MarkSeedWindow.FROM))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class MarkRetrieval {

		@Test
		void seedWindowReturnsBuyAndSellMarks() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSeededMarks())
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.isArray()).isTrue();
			assertThat(root).hasSize(3);

			JsonNode first = root.get(0);
			assertThat(first.get("id").asText()).isEqualTo("m1");
			assertThat(first.get("time").asLong()).isEqualTo(1_787_011_200L);
			assertThat(first.get("color").asText()).isEqualTo("green");
			assertThat(first.get("label").asText()).isEqualTo("B");
			assertThat(first.get("text").asText()).isEqualTo("Buy signal");
			assertThat(first.get("labelFontColor").asText()).isEqualTo("#ffffff");
			assertThat(first.get("minSize").asInt()).isEqualTo(14);

			assertThat(root.get(1).get("color").asText()).isEqualTo("red");
			assertThat(root.get(1).get("label").asText()).isEqualTo("S");

			for (JsonNode mark : root) {
				long time = mark.get("time").asLong();
				assertThat(time).isBetween(MarkSeedWindow.FROM, MarkSeedWindow.TO);
			}
		}

		@Test
		void displaySymbolStillReturnsMarks() throws Exception {
			mockMvc.perform(authorizedMarks("USD/JPY", "1D", MarkSeedWindow.FROM, MarkSeedWindow.TO))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(3));
		}

		@Test
		void rangeWithNoMarksReturnsEmptyArray() throws Exception {
			MvcResult result = mockMvc.perform(authorizedMarks("USDJPY", "1D", 1L, 2L))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.isArray()).isTrue();
			assertThat(root).isEmpty();
		}

		@Test
		void wrongResolutionReturnsEmptyArray() throws Exception {
			MvcResult result = mockMvc.perform(
							authorizedMarks("USDJPY", "60", MarkSeedWindow.FROM, MarkSeedWindow.TO))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root).isEmpty();
		}
	}

	@Nested
	class ConfigFlag {

		@Test
		void supportsMarksTrue() throws Exception {
			MvcResult result = mockMvc.perform(get("/api/config").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("supports_marks").asBoolean()).isTrue();
		}
	}

	private MockHttpServletRequestBuilder seededMarks() {
		return get("/api/marks")
				.param("symbol", "USDJPY")
				.param("resolution", "1D")
				.param("from", String.valueOf(MarkSeedWindow.FROM))
				.param("to", String.valueOf(MarkSeedWindow.TO));
	}

	private MockHttpServletRequestBuilder authorizedSeededMarks() {
		return seededMarks().header(HttpHeaders.AUTHORIZATION, bearerDemo);
	}

	private MockHttpServletRequestBuilder authorizedMarks(
			String symbol,
			String resolution,
			long from,
			long to) {
		return get("/api/marks")
				.param("symbol", symbol)
				.param("resolution", resolution)
				.param("from", String.valueOf(from))
				.param("to", String.valueOf(to))
				.header(HttpHeaders.AUTHORIZATION, bearerDemo);
	}
}
