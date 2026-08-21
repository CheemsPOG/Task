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
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /api/history against design doc 121 (validation + BID/ASK/MID bars).
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
@SpringBootTest
@AutoConfigureMockMvc
class SystemOverviewDesign121Test {

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
			mockMvc.perform(get("/api/history")
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("countBack", "10"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class ValidationCheck {

		@Test
		void missingSymbolReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("resolution", "1D")
							.param("countBack", "10")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void blankSymbolReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "  ")
							.param("resolution", "1D")
							.param("countBack", "10")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void unsupportedResolutionReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "2")
							.param("countBack", "10")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void invalidBidAskReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("countBack", "10")
							.param("bid_ask", "FOO"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void fromWithoutToReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("from", "1721037907")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void toWithoutFromIsAllowedWithCountBack() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("to", "1721037907")
							.param("countBack", "5")
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.s").value("ok"));
		}

		@Test
		void toBeforeFromReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("from", "1721038000")
							.param("to", "1721037907")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class HistoricalDataRetrieval {

		@Test
		void returnsOkBarsForWidgetPriceParam() throws Exception {
			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("countBack", "10")
							.param("price", "mid"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.path("s").asText()).isEqualTo("ok");
			assertThat(root.path("bars").isArray()).isTrue();
			assertThat(root.path("bars")).hasSize(10);
			assertBarShape(root.path("bars").get(0));
			assertThat(root.path("bars").get(9).path("time").asLong())
					.isLessThanOrEqualTo(Instant.now().toEpochMilli());
		}

		@Test
		void futureToIsClampedSoLastBarIsNotAfterNow() throws Exception {
			long futureTo = Instant.now().getEpochSecond() + 10 * 86_400L;

			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("to", String.valueOf(futureTo))
							.param("countBack", "5")
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode bars = objectMapper.readTree(result.getResponse().getContentAsString()).path("bars");
			assertThat(bars).hasSize(5);
			long lastTime = bars.get(bars.size() - 1).path("time").asLong();
			long currentOpen = Math.floorDiv(Instant.now().toEpochMilli() - 1, 86_400_000L) * 86_400_000L;
			assertThat(lastTime).isLessThanOrEqualTo(currentOpen);
		}

		@Test
		void bidAskAskProducesHigherCloseThanMid() throws Exception {
			long to = Instant.parse("2026-01-01T00:00:00Z").getEpochSecond();

			JsonNode mid = historyJson("MID", to);
			JsonNode ask = historyJson("ASK", to);
			JsonNode bid = historyJson("BID", to);

			assertThat(ask.path("bars").get(0).path("close").asDouble())
					.isGreaterThan(mid.path("bars").get(0).path("close").asDouble());
			assertThat(bid.path("bars").get(0).path("close").asDouble())
					.isLessThan(mid.path("bars").get(0).path("close").asDouble());
		}

		@Test
		void tenMinuteResolutionIsAccepted() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "10")
							.param("countBack", "5")
							.param("bid_ask", "BID"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.s").value("ok"))
					.andExpect(jsonPath("$.bars.length()").value(5));
		}

		@Test
		void fromAndToFilterBarsAscending() throws Exception {
			long to = Instant.parse("2026-01-01T00:00:00Z").getEpochSecond();
			long from = to - 10 * 86_400L;

			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode bars = objectMapper.readTree(result.getResponse().getContentAsString()).path("bars");
			assertThat(bars.isArray()).isTrue();
			assertThat(bars.size()).isPositive();
			long previous = Long.MIN_VALUE;
			for (JsonNode bar : bars) {
				long time = bar.path("time").asLong();
				assertThat(time).isGreaterThanOrEqualTo(from * 1000L);
				assertThat(time).isLessThan(to * 1000L);
				assertThat(time).isGreaterThan(previous);
				previous = time;
			}
		}

		@Test
		void unknownSymbolReturnsUdfErrorBodyNot422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "ETHUSD")
							.param("resolution", "1D")
							.param("countBack", "10")
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.s").value("error"))
					.andExpect(jsonPath("$.errmsg").value("unknown_symbol"));
		}

		@Test
		void returnsWidgetBarsAndPeachColumnarArrays() throws Exception {
			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("countBack", "3")
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.path("s").asText()).isEqualTo("ok");
			assertThat(root.path("bars")).hasSize(3);
			assertThat(root.path("t")).hasSize(3);
			assertThat(root.path("o")).hasSize(3);
			assertThat(root.path("h")).hasSize(3);
			assertThat(root.path("l")).hasSize(3);
			assertThat(root.path("c")).hasSize(3);
			assertThat(root.path("bars").get(0).has("time")).isTrue();
			assertThat(root.path("t").get(0).asLong())
					.isEqualTo(root.path("bars").get(0).path("time").asLong() / 1000L);
			assertThat(root.path("o").get(0).asDouble())
					.isEqualTo(root.path("bars").get(0).path("open").asDouble());
		}

		@Test
		void emptyFutureRangeReturnsNoDataWithNextTime() throws Exception {
			long from = Instant.now().getEpochSecond() + 10 * 86_400L;
			long to = from + 86_400L;

			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.path("s").asText()).isEqualTo("no_data");
			assertThat(root.path("noData").asBoolean()).isTrue();
			assertThat(root.path("bars")).isEmpty();
			assertThat(root.path("t")).isEmpty();
			assertThat(root.has("nextTime")).isTrue();
			long nextTime = root.path("nextTime").asLong();
			assertThat(nextTime).isLessThan(from);
			assertThat(nextTime).isPositive();
		}

		private JsonNode historyJson(String bidAsk, long to) throws Exception {
			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("to", String.valueOf(to))
							.param("countBack", "1")
							.param("bid_ask", bidAsk))
					.andExpect(status().isOk())
					.andReturn();
			return objectMapper.readTree(result.getResponse().getContentAsString());
		}
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorizedHistory() {
		return get("/api/history").header(HttpHeaders.AUTHORIZATION, bearerDemo);
	}

	private static void assertBarShape(JsonNode bar) {
		assertThat(bar.has("time")).isTrue();
		assertThat(bar.has("open")).isTrue();
		assertThat(bar.has("high")).isTrue();
		assertThat(bar.has("low")).isTrue();
		assertThat(bar.has("close")).isTrue();
		assertThat(bar.has("volume")).isTrue();
		assertThat(bar.path("high").asDouble())
				.isGreaterThanOrEqualTo(Math.max(bar.path("open").asDouble(), bar.path("close").asDouble()));
		assertThat(bar.path("low").asDouble())
				.isLessThanOrEqualTo(Math.min(bar.path("open").asDouble(), bar.path("close").asDouble()));
	}
}
