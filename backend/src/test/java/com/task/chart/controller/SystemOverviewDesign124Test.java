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
import com.task.chart.entity.Ccypair;
import com.task.chart.repository.CcypairRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /api/search against design doc 124 (token, validation, m_ccypairs list).
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
class SystemOverviewDesign124Test {

	@Autowired
	private MockMvc mockMvc;

	private String bearerDemo;
	private String bearerDemo2;

	@BeforeEach
	void authenticateDemoUsers() throws Exception {
		bearerDemo = TestAuthSupport.bearerDemo(mockMvc);
		bearerDemo2 = TestAuthSupport.bearerDemo2(mockMvc);
	}

	@Autowired
	private CcypairRepository ccypairRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/search")).andExpect(status().isUnauthorized());
		}

		@Test
		void validTokenReturns200() throws Exception {
			mockMvc.perform(authorizedSearch()).andExpect(status().isOk());
		}
	}

	@Nested
	class RequestValidation {

		@Test
		void queryLongerThanTenReturns422() throws Exception {
			mockMvc.perform(authorizedSearch().param("query", "ABCDEFGHIJK"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void limitZeroReturns422() throws Exception {
			mockMvc.perform(authorizedSearch().param("limit", "0"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void limitAboveMaxReturns422() throws Exception {
			mockMvc.perform(authorizedSearch().param("limit", "101"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class CurrencyPairMasterRetrieval {

		@Test
		void emptyQueryReturnsAllActiveOrderedByPriority() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSearch())
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.isArray()).isTrue();
			assertThat(root).hasSize(5);

			List<String> codes = new ArrayList<>();
			root.forEach(node -> codes.add(node.get("symbol").asText()));
			assertThat(codes).containsExactly("USDJPY", "EURJPY", "EURUSD", "GBPUSD", "AUDUSD");
		}

		@Test
		void queryUsdMatchesCcypairCd() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSearch().param("query", "USD"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			List<String> codes = new ArrayList<>();
			root.forEach(node -> codes.add(node.get("symbol").asText()));
			assertThat(codes).contains("USDJPY");
			assertThat(codes).doesNotContain("EURJPY");
		}

		@Test
		void queryJapaneseMatchesCcypairJp() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSearch().param("query", "円"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.size()).isGreaterThanOrEqualTo(2);
			for (JsonNode node : root) {
				assertThat(node.get("description").asText()).contains("円");
			}
		}

		@Test
		void slashQueryStillMatchesCd() throws Exception {
			mockMvc.perform(authorizedSearch().param("query", "USD/JPY"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[0].symbol").value("USDJPY"));
		}

		@Test
		void widgetCtfxForexFilterStillReturnsFive() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSearch()
							.param("exchange", "CTFX")
							.param("type", "FOREX"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root).hasSize(5);
		}

		@Test
		@Transactional
		void deletedPairIsExcluded() throws Exception {
			Ccypair pair = ccypairRepository.findById("AUDUSD").orElseThrow();
			pair.setIsDeleted(1);
			ccypairRepository.saveAndFlush(pair);

			MvcResult result = mockMvc.perform(authorizedSearch())
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			List<String> codes = new ArrayList<>();
			root.forEach(node -> codes.add(node.get("symbol").asText()));
			assertThat(codes).doesNotContain("AUDUSD");
			assertThat(codes).hasSize(4);
		}
	}

	@Nested
	class CurrencyPairMasterDtoMapping {

		@Test
		void mapsDocFieldsAndWidgetExtras() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSearch().param("query", "USDJPY"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root).hasSize(1);
			JsonNode hit = root.get(0);
			assertThat(hit.get("symbol").asText()).isEqualTo("USDJPY");
			assertThat(hit.get("description").asText()).isEqualTo("米ドル/円");
			assertThat(hit.get("type").asText()).isEqualTo("FOREX");
			assertThat(hit.get("exchange").asText()).isEqualTo("CTFX");
			assertThat(hit.get("ticker").asText()).isEqualTo("USD/JPY");
			assertThat(hit.get("full_name").asText()).isEqualTo("USD/JPY");
		}
	}

	private MockHttpServletRequestBuilder authorizedSearch() {
		return get("/api/search").header(HttpHeaders.AUTHORIZATION, bearerDemo);
	}
}
