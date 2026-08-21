/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.support.TestAuthSupport;
import com.task.chart.entity.TvChartLayout;
import com.task.chart.repository.TvChartLayoutRepository;
import java.time.Instant;
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

/**
 * PUT /api/layouts/{id} against design doc 128 (token, path id, validation, update).
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
class SystemOverviewDesign128Test {

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

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			long id = seedLayout(1L);
			mockMvc.perform(updateBody(id, "Renamed", "{\"pane\":2}", "EURUSD", "60")
							.contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class PathParameterCheck {

		@Test
		void nonNumericIdReturns422() throws Exception {
			mockMvc.perform(authorizedUpdate("abc", "Renamed", "{\"pane\":2}", "EURUSD", "60"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class ValidationCheck {

		@Test
		void blankNameReturns422() throws Exception {
			long id = seedLayout(1L);
			mockMvc.perform(authorizedUpdate(String.valueOf(id), "  ", "{\"pane\":2}", "EURUSD", "60"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void unsupportedResolutionReturns422() throws Exception {
			long id = seedLayout(1L);
			mockMvc.perform(authorizedUpdate(String.valueOf(id), "Renamed", "{\"pane\":2}", "EURUSD", "10"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class ExistenceAndPairCheck {

		@Test
		void unknownIdReturns404() throws Exception {
			mockMvc.perform(authorizedUpdate("999999", "Renamed", "{\"pane\":2}", "EURUSD", "60"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void unknownSymbolReturns404() throws Exception {
			long id = seedLayout(1L);
			mockMvc.perform(authorizedUpdate(String.valueOf(id), "Renamed", "{\"pane\":2}", "ETHUSD", "60"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void otherCustomerReturns404() throws Exception {
			long id = seedLayout(1L);
			mockMvc.perform(updateBody(id, "Renamed", "{\"pane\":2}", "EURUSD", "60")
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.AUTHORIZATION, bearerDemo2))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}
	}

	@Nested
	class Update {

		@Test
		void happyPathUpdatesRowAndReturnsSameId() throws Exception {
			long id = seedLayout(1L);
			Instant before = tvChartLayoutRepository.findById(id).orElseThrow().getUpdatedAt();

			Thread.sleep(20);

			MvcResult result = mockMvc.perform(authorizedUpdate(
							String.valueOf(id),
							"Renamed",
							"{\"pane\":2}",
							"EURUSD",
							"60"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(id))
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("id").asLong()).isEqualTo(id);

			TvChartLayout saved = tvChartLayoutRepository.findById(id).orElseThrow();
			assertThat(saved.getName()).isEqualTo("Renamed");
			assertThat(saved.getContent()).isEqualTo("{\"pane\":2}");
			assertThat(saved.getCcypairCd()).isEqualTo("EURUSD");
			assertThat(saved.getChartType()).isEqualTo("60");
			assertThat(saved.getCustomerNo()).isEqualTo(1L);
			assertThat(saved.getUpdatedAt()).isAfter(before);
		}

		@Test
		void displaySymbolNormalizesOnUpdate() throws Exception {
			long id = seedLayout(1L);
			mockMvc.perform(authorizedUpdate(String.valueOf(id), "Slash", "{\"x\":1}", "EUR/USD", "1D"))
					.andExpect(status().isOk());

			TvChartLayout saved = tvChartLayoutRepository.findById(id).orElseThrow();
			assertThat(saved.getCcypairCd()).isEqualTo("EURUSD");
		}
	}

	private long seedLayout(long customerNo) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/layouts")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, customerNo == 1L ? bearerDemo : bearerDemo2)
						.content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
								.put("name", "Seed")
								.put("content", "{\"pane\":1}")
								.put("symbol", "USDJPY")
								.put("resolution", "1D"))))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private MockHttpServletRequestBuilder authorizedUpdate(
			String id,
			String name,
			String content,
			String symbol,
			String resolution) throws Exception {
		return updateBody(id, name, content, symbol, resolution)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, bearerDemo);
	}

	private MockHttpServletRequestBuilder updateBody(
			Object id,
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
		return put("/api/layouts/" + id).content(objectMapper.writeValueAsString(node));
	}
}
