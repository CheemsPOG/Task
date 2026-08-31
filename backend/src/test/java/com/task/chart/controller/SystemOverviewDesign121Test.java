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
import com.task.chart.cache.CacheNamespace;
import com.task.chart.cache.ChartBarRepository;
import com.task.chart.cache.ChartCacheStore;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.support.TestAuthSupport;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
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
 * GET /api/history against design doc 121 Phase 1 (in-memory Peach caches).
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
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Phase 1 cache read path</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/27</td><td>Task</td><td>Doc 121 columnar JSON only</td></tr>
 *   <tr><td>1.3.0</td><td>2026/08/30</td><td>Task</td><td>Saturday 1D has bars (24/7 mock)</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.3.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SystemOverviewDesign121Test {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChartCacheStore chartCacheStore;

	@Autowired
	private ChartBarRepository chartBarRepository;

	private String bearerDemo;

	@BeforeEach
	void authenticateDemoUsers() throws Exception {
		bearerDemo = TestAuthSupport.bearerDemo(mockMvc);
	}

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/history")
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", "1700000000")
							.param("to", "1700100000")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class ValidationCheck {

		@Test
		void missingSymbolReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("resolution", "1D")
							.param("from", "1700000000")
							.param("to", "1700100000")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void blankSymbolReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "  ")
							.param("resolution", "1D")
							.param("from", "1700000000")
							.param("to", "1700100000")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void symbolNotLengthSixAfterNormalizeReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJP")
							.param("resolution", "1D")
							.param("from", "1700000000")
							.param("to", "1700100000")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void unsupportedResolutionReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "2")
							.param("from", "1700000000")
							.param("to", "1700100000")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void missingBidAskReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", "1700000000")
							.param("to", "1700100000"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void invalidBidAskReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", "1700000000")
							.param("to", "1700100000")
							.param("bid_ask", "FOO"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void fromWithoutToReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", "1721037907")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void toWithoutFromReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("to", "1721037907")
							.param("countBack", "5")
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void toBeforeFromReturns422() throws Exception {
			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
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
		void cacheIsSeededForDayNamespace() {
			assertThat(chartCacheStore.size(CacheNamespace.CACHE_SET_DAY, "USDJPY")).isPositive();
			assertThat(chartBarRepository.size(CacheNamespace.CACHE_SET_DAY, "USDJPY")).isPositive();
			assertThat(chartBarRepository.size(CacheNamespace.CACHE_SET_DAY, "USDJPY"))
					.isEqualTo(chartCacheStore.size(CacheNamespace.CACHE_SET_DAY, "USDJPY"));
		}

		@Test
		void returnsOkBarsWithBidAskAndSlashSymbol() throws Exception {
			long to = Instant.now().getEpochSecond();
			long from = to - 20 * 86_400L;

			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USD/JPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("countBack", "10")
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.path("s").asText()).isEqualTo("ok");
			assertNoExtraHistoryFields(root);
			assertThat(root.path("t").size()).isBetween(1, 10);
			assertColumnarShape(root);
			assertThat(root.path("t").get(root.path("t").size() - 1).asLong())
					.isLessThanOrEqualTo(Instant.now().getEpochSecond());
		}

		@Test
		void futureToIsClampedSoLastBarIsNotAfterNow() throws Exception {
			long to = Instant.now().getEpochSecond() + 10 * 86_400L;
			long from = Instant.now().getEpochSecond() - 10 * 86_400L;

			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("countBack", "5")
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode times = objectMapper.readTree(result.getResponse().getContentAsString()).path("t");
			assertThat(times.size()).isBetween(1, 5);
			long lastTime = times.get(times.size() - 1).asLong();
			long currentOpenSec = Math.floorDiv(Instant.now().getEpochSecond() - 1, 86_400L) * 86_400L;
			assertThat(lastTime).isLessThanOrEqualTo(currentOpenSec);
		}

		@Test
		void midCloseIsAverageOfBidAndAskClose() throws Exception {
			long to = Instant.parse("2026-01-02T00:00:00Z").getEpochSecond();
			long from = to - 86_400L;

			JsonNode mid = historyJson("MID", from, to);
			JsonNode ask = historyJson("ASK", from, to);
			JsonNode bid = historyJson("BID", from, to);

			double midClose = mid.path("c").get(0).asDouble();
			double askClose = ask.path("c").get(0).asDouble();
			double bidClose = bid.path("c").get(0).asDouble();
			assertThat(midClose).isEqualTo((askClose + bidClose) / 2.0);
			assertThat(askClose).isGreaterThan(bidClose);
		}

		@Test
		void tenMinuteResolutionIsAccepted() throws Exception {
			long to = Instant.now().getEpochSecond();
			long from = to - 10 * 600L;

			mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "10")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("countBack", "5")
							.param("bid_ask", "BID"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.s").value("ok"))
					.andExpect(jsonPath("$.t.length()").value(5));
		}

		@Test
		void fromAndToFilterBarsAscendingInclusive() throws Exception {
			long to = Instant.parse("2026-01-02T00:00:00Z").getEpochSecond();
			long from = to - 10 * 86_400L;

			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			JsonNode times = root.path("t");
			assertThat(times.isArray()).isTrue();
			assertThat(times.size()).isPositive();
			assertNoExtraHistoryFields(root);
			long previous = Long.MIN_VALUE;
			for (JsonNode timeNode : times) {
				long time = timeNode.asLong();
				assertThat(time).isGreaterThanOrEqualTo(from);
				assertThat(time).isLessThanOrEqualTo(to);
				assertThat(time).isGreaterThan(previous);
				previous = time;
			}
		}

		@Test
		void unknownSymbolReturns422() throws Exception {
			long to = Instant.now().getEpochSecond();
			long from = to - 86_400L;

			mockMvc.perform(authorizedHistory()
							.param("symbol", "ETHUSD")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("bid_ask", "MID"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void returnsPeachColumnarArraysWithoutWidgetBars() throws Exception {
			long to = Instant.now().getEpochSecond();
			long from = to - 5 * 86_400L;

			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("countBack", "3")
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.path("s").asText()).isEqualTo("ok");
			assertNoExtraHistoryFields(root);
			assertThat(root.path("t").size()).isBetween(1, 3);
			assertColumnarShape(root);
			assertThat(root.has("nextTime")).isFalse();
		}

		@Test
		void saturdayDailyRangeReturnsSeededBars() throws Exception {
			LocalDate saturday = LocalDate.now(ZoneOffset.UTC)
					.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
			long from = saturday.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
			long to = from + 86_400L - 1;

			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
							.param("to", String.valueOf(to))
							.param("bid_ask", "MID"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.path("s").asText()).isEqualTo("ok");
			assertNoExtraHistoryFields(root);
			assertThat(root.path("t").size()).isGreaterThanOrEqualTo(1);
			assertColumnarShape(root);
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
			assertNoExtraHistoryFields(root);
			assertThat(root.path("t")).isEmpty();
			assertThat(root.path("o")).isEmpty();
			assertThat(root.path("h")).isEmpty();
			assertThat(root.path("l")).isEmpty();
			assertThat(root.path("c")).isEmpty();
			assertThat(root.has("nextTime")).isTrue();
			long nextTime = root.path("nextTime").asLong();
			assertThat(nextTime).isLessThan(from);
			assertThat(nextTime).isPositive();
		}

		private JsonNode historyJson(String bidAsk, long from, long to) throws Exception {
			MvcResult result = mockMvc.perform(authorizedHistory()
							.param("symbol", "USDJPY")
							.param("resolution", "1D")
							.param("from", String.valueOf(from))
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

	private static void assertNoExtraHistoryFields(JsonNode root) {
		assertThat(root.has("bars")).isFalse();
		assertThat(root.has("noData")).isFalse();
		assertThat(root.has("errmsg")).isFalse();
	}

	private static void assertColumnarShape(JsonNode root) {
		int size = root.path("t").size();
		assertThat(root.path("o")).hasSize(size);
		assertThat(root.path("h")).hasSize(size);
		assertThat(root.path("l")).hasSize(size);
		assertThat(root.path("c")).hasSize(size);
		double open = root.path("o").get(0).asDouble();
		double high = root.path("h").get(0).asDouble();
		double low = root.path("l").get(0).asDouble();
		double close = root.path("c").get(0).asDouble();
		assertThat(high).isGreaterThanOrEqualTo(Math.max(open, close));
		assertThat(low).isLessThanOrEqualTo(Math.min(open, close));
	}
}
