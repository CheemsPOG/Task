/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.support.TestAuthSupport;
import com.task.chart.entity.Ccypair;
import com.task.chart.entity.TvChartLayout;
import com.task.chart.repository.CcypairRepository;
import com.task.chart.repository.TvChartLayoutRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * POST /api/layouts against design doc 127 (token, validation, pair, register).
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
class SystemOverviewDesign127Test {

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
	private TvChartLayoutRepository tvChartLayoutRepository;

	@Autowired
	private CcypairRepository ccypairRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(registerBody(
							"My layout",
							"{\"pane\":1}",
							"USDJPY",
							"1D")
							.contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class ValidationCheck {

		@Test
		void missingNameReturns422() throws Exception {
			mockMvc.perform(authorizedRegister(
							null,
							"{\"pane\":1}",
							"USDJPY",
							"1D"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void blankContentReturns422() throws Exception {
			mockMvc.perform(authorizedRegister("My layout", "  ", "USDJPY", "1D"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void blankSymbolReturns422() throws Exception {
			mockMvc.perform(authorizedRegister("My layout", "{\"pane\":1}", "", "1D"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void missingResolutionReturns422() throws Exception {
			mockMvc.perform(authorizedRegister("My layout", "{\"pane\":1}", "USDJPY", null))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void nameLongerThan64Returns422() throws Exception {
			String longName = "a".repeat(65);
			mockMvc.perform(authorizedRegister(longName, "{\"pane\":1}", "USDJPY", "1D"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void unsupportedResolutionReturns422() throws Exception {
			mockMvc.perform(authorizedRegister("My layout", "{\"pane\":1}", "USDJPY", "10"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class PairCheck {

		@Test
		void unknownSymbolReturns404() throws Exception {
			mockMvc.perform(authorizedRegister("My layout", "{\"pane\":1}", "ETHUSD", "1D"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		@Transactional
		void deletedPairReturns404() throws Exception {
			Ccypair pair = ccypairRepository.findById("USDJPY").orElseThrow();
			pair.setIsDeleted(1);
			ccypairRepository.save(pair);

			mockMvc.perform(authorizedRegister("My layout", "{\"pane\":1}", "USDJPY", "1D"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}
	}

	@Nested
	class Registration {

		@Test
		void happyPathPersistsRowAndReturnsId() throws Exception {
			MvcResult result = mockMvc.perform(authorizedRegister(
							"My layout",
							"{\"pane\":1}",
							"USDJPY",
							"1D"))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.id").isNumber())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			long id = root.get("id").asLong();
			assertThat(id).isPositive();

			TvChartLayout saved = tvChartLayoutRepository.findById(id).orElseThrow();
			assertThat(saved.getCustomerNo()).isEqualTo(1L);
			assertThat(saved.getName()).isEqualTo("My layout");
			assertThat(saved.getContent()).isEqualTo("{\"pane\":1}");
			assertThat(saved.getCcypairCd()).isEqualTo("USDJPY");
			assertThat(saved.getChartType()).isEqualTo("1D");
			assertThat(saved.getUpdatedAt()).isNotNull();
		}

		@Test
		void displaySymbolNormalizesToCcypairCd() throws Exception {
			MvcResult result = mockMvc.perform(authorizedRegister(
							"Slash layout",
							"{\"pane\":2}",
							"USD/JPY",
							"60"))
					.andExpect(status().isCreated())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			long id = root.get("id").asLong();
			TvChartLayout saved = tvChartLayoutRepository.findById(id).orElseThrow();
			assertThat(saved.getCcypairCd()).isEqualTo("USDJPY");
			assertThat(saved.getChartType()).isEqualTo("60");
		}
	}

	private MockHttpServletRequestBuilder authorizedRegister(
			String name,
			String content,
			String symbol,
			String resolution) throws Exception {
		return registerBody(name, content, symbol, resolution)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, bearerDemo);
	}

	private MockHttpServletRequestBuilder registerBody(
			String name,
			String content,
			String symbol,
			String resolution) throws Exception {
		var node = objectMapper.createObjectNode();
		if (name != null) {
			node.put("name", name);
		}
		if (content != null) {
			node.put("content", content);
		}
		if (symbol != null) {
			node.put("symbol", symbol);
		}
		if (resolution != null) {
			node.put("resolution", resolution);
		}
		return post("/api/layouts").content(objectMapper.writeValueAsString(node));
	}
}
