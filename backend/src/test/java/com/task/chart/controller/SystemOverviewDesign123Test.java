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
import com.task.chart.config.AppProperties;
import com.task.chart.constants.ErrorCodes;
import com.task.chart.support.TestAuthSupport;
import com.task.chart.entity.Ccypair;
import com.task.chart.entity.Season;
import com.task.chart.repository.CcypairRepository;
import com.task.chart.repository.SeasonRepository;
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
 * GET /api/symbols against design doc 123 (token, validation, m_ccypairs, m_season).
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
class SystemOverviewDesign123Test {

	private static final List<String> DOC_RESOLUTIONS = List.of(
			"1S", "1", "5", "15", "30", "60", "120", "240", "480", "1D", "1W", "1M");

	private static final List<String> DOC_INTRADAY_MULTIPLIERS = List.of(
			"1", "5", "15", "30", "60", "120", "240", "480");

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
	private AppProperties appProperties;

	@Autowired
	private CcypairRepository ccypairRepository;

	@Autowired
	private SeasonRepository seasonRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/symbols").param("symbol", "USDJPY"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void validTokenReturns200() throws Exception {
			mockMvc.perform(authorizedSymbols("USDJPY"))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class RequestValidation {

		@Test
		void missingSymbolReturns422() throws Exception {
			mockMvc.perform(get("/api/symbols").header(HttpHeaders.AUTHORIZATION, bearerDemo))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}

		@Test
		void blankSymbolReturns422() throws Exception {
			mockMvc.perform(authorizedSymbols("   "))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.VALIDATION));
		}
	}

	@Nested
	class CurrencyPairRetrieval {

		@Test
		void usdJpyFromMasterReturns200() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSymbols("USDJPY"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("name").asText()).isEqualTo("USD/JPY");
			assertThat(root.get("description").asText()).isEqualTo("米ドル/円");
			assertThat(root.get("pricescale").asInt()).isEqualTo(1000);
		}

		@Test
		void widgetDisplayNameStillResolves() throws Exception {
			mockMvc.perform(authorizedSymbols("USD/JPY"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.name").value("USD/JPY"));
		}

		@Test
		void fxPrefixedDisplayNameStillResolves() throws Exception {
			mockMvc.perform(authorizedSymbols("FX:USD/JPY"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.name").value("USD/JPY"));
		}

		@Test
		void unknownSymbolReturns404() throws Exception {
			mockMvc.perform(authorizedSymbols("ETHUSD"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		@Transactional
		void deletedPairReturns404() throws Exception {
			Ccypair pair = ccypairRepository.findById("AUDUSD").orElseThrow();
			pair.setIsDeleted(1);
			ccypairRepository.saveAndFlush(pair);

			mockMvc.perform(authorizedSymbols("AUDUSD"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.NOT_FOUND));
		}

		@Test
		void nonYenPairUsesRateUnitFive() throws Exception {
			mockMvc.perform(authorizedSymbols("EURUSD"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.name").value("EUR/USD"))
					.andExpect(jsonPath("$.pricescale").value(100_000))
					.andExpect(jsonPath("$.description").value("ユーロ/米ドル"));
		}
	}

	@Nested
	class CurrencyPairMasterDtoMapping {

		@Test
		void mapsEveryDocFieldFromMasterAndExternalConfiguration() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSymbols("USDJPY"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.has("success")).isFalse();
			List<String> fieldNames = new ArrayList<>();
			root.fieldNames().forEachRemaining(fieldNames::add);
			assertThat(fieldNames).contains(
					"name",
					"description",
					"timezone",
					"exchange",
					"minmov",
					"pricescale",
					"type",
					"session",
					"has_intraday",
					"visible_plots_set",
					"supported_resolutions",
					"intraday_multipliers",
					"has_seconds");

			AppProperties.TradingView tradingView = appProperties.getTradingView();
			assertThat(root.get("name").asText()).isEqualTo("USD/JPY");
			assertThat(root.get("description").asText()).isEqualTo("米ドル/円");
			assertThat(root.get("timezone").asText()).isEqualTo("Asia/Tokyo");
			assertThat(root.get("exchange").asText()).isEqualTo("CTFX");
			assertThat(root.get("minmov").asInt()).isEqualTo(1);
			assertThat(root.get("pricescale").asInt()).isEqualTo(1000);
			assertThat(root.get("type").asText()).isEqualTo("FOREX");
			assertThat(root.get("session").asText()).isEqualTo(tradingView.getTimeWinter());
			assertThat(root.get("has_intraday").asBoolean()).isTrue();
			assertThat(root.get("visible_plots_set").asText()).isEqualTo("ohlc");
			assertThat(root.get("has_seconds").asBoolean()).isTrue();
			assertThat(root.get("provider_symbol").asText()).isEqualTo("USDJPY");

			List<String> resolutions = new ArrayList<>();
			root.get("supported_resolutions").forEach(node -> resolutions.add(node.asText()));
			assertThat(resolutions).containsExactlyElementsOf(DOC_RESOLUTIONS);

			List<String> multipliers = new ArrayList<>();
			root.get("intraday_multipliers").forEach(node -> multipliers.add(node.asText()));
			assertThat(multipliers).containsExactlyElementsOf(DOC_INTRADAY_MULTIPLIERS);
		}
	}

	@Nested
	class SeasonSession {

		@Test
		void seedStandardTimeUsesTimeWinter() throws Exception {
			MvcResult result = mockMvc.perform(authorizedSymbols("USDJPY"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("session").asText())
					.isEqualTo(appProperties.getTradingView().getTimeWinter());
		}

		@Test
		@Transactional
		void daylightSavingUsesTimeSummer() throws Exception {
			Season season = seasonRepository.findAll().get(0);
			season.setSeasonCd(Season.DAYLIGHT_SAVING);
			seasonRepository.saveAndFlush(season);

			MvcResult result = mockMvc.perform(authorizedSymbols("USDJPY"))
					.andExpect(status().isOk())
					.andReturn();

			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(root.get("session").asText())
					.isEqualTo(appProperties.getTradingView().getTimeSummer());
		}

		@Test
		@Transactional
		void noMatchingSeasonReturns500() throws Exception {
			seasonRepository.deleteAll();
			seasonRepository.flush();

			mockMvc.perform(authorizedSymbols("USDJPY"))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.SERVER))
					.andExpect(jsonPath("$.errorCode").value(ErrorCodes.SERVER));
		}
	}

	private MockHttpServletRequestBuilder authorizedSymbols(String symbol) {
		return get("/api/symbols")
				.param("symbol", symbol)
				.header(HttpHeaders.AUTHORIZATION, bearerDemo);
	}
}
