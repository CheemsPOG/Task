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
import com.task.chart.dto.response.DatafeedConfigResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /api/config against design doc 120 (token check + external configuration mapping).
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
class SystemOverviewDesign120Test {

	private static final List<String> DOC_RESOLUTIONS = List.of(
			"1S", "1", "5", "15", "30", "60", "120", "240", "480", "1D", "1W", "1M");

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	class TokenAuthentication {

		@Autowired
		private MockMvc mockMvc;

		private String bearerDemo;

		@BeforeEach
		void authenticateDemoUsers() throws Exception {
			bearerDemo = TestAuthSupport.bearerDemo(mockMvc);
		}

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/config")).andExpect(status().isUnauthorized());
		}

		@Test
		void invalidBearerReturns401() throws Exception {
			mockMvc.perform(get("/api/config").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.UNAUTHORIZED));
		}

		@Test
		void validTokenReturns200() throws Exception {
			mockMvc.perform(get("/api/config").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk());
		}
	}

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@ActiveProfiles("doc120")
	class ExternalConfigurationMapping {

		@Autowired
		private MockMvc mockMvc;

		private final ObjectMapper objectMapper = new ObjectMapper();

		private String bearerDemo;

		@BeforeEach
		void authenticateDemoUsers() throws Exception {
			bearerDemo = TestAuthSupport.bearerDemo(mockMvc);
		}

		@Test
		void mapsEveryDatafeedConfigurationDtoFieldFromExternalConfiguration() throws Exception {
			MvcResult result = mockMvc.perform(get("/api/config").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk())
					.andReturn();

			String body = result.getResponse().getContentAsString();
			JsonNode root = objectMapper.readTree(body);
			assertThat(root.has("success")).isFalse();
			assertThat(root.has("data")).isFalse();
			List<String> fieldNames = new ArrayList<>();
			root.fieldNames().forEachRemaining(fieldNames::add);
			assertThat(fieldNames).contains(
					"supports_search",
					"supports_marks",
					"supports_timescale_marks",
					"supports_time",
					"exchanges",
					"symbols_types",
					"supported_resolutions");

			DatafeedConfigResponse config = objectMapper.readValue(body, DatafeedConfigResponse.class);
			assertThat(config.supports_search()).isTrue();
			assertThat(config.supports_marks()).isTrue();
			assertThat(config.supports_timescale_marks()).isTrue();
			assertThat(config.supports_time()).isTrue();
			assertThat(config.supported_resolutions()).containsExactlyElementsOf(DOC_RESOLUTIONS);
			assertThat(config.exchanges()).hasSize(1);
			assertThat(config.exchanges().get(0).value()).isEqualTo("CTFX");
			assertThat(config.exchanges().get(0).name()).isEqualTo("CTFX");
			assertThat(config.exchanges().get(0).desc()).isEqualTo("CTFX");
			assertThat(config.symbols_types()).hasSize(1);
			assertThat(config.symbols_types().get(0).name()).isEqualTo("FOREX");
			assertThat(config.symbols_types().get(0).value()).isEqualTo("FOREX");
			assertThat(config.supports_group_request()).isFalse();
		}

		@Test
		void doesNotRequireDatabaseTables() throws Exception {
			mockMvc.perform(get("/api/config").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isOk());
		}
	}
}
