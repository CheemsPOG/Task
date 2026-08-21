/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.chart.constants.ApiHeaders;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /api/time against design doc 122 (server epoch seconds as {@code t}).
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
class SystemOverviewDesign122Test {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Nested
	class TokenAuthentication {

		@Test
		void missingTokenReturns401() throws Exception {
			mockMvc.perform(get("/api/time")).andExpect(status().isUnauthorized());
		}

		@Test
		void validTokenReturns200() throws Exception {
			mockMvc.perform(get("/api/time").header(ApiHeaders.CUSTOMER_NO, "1"))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class MapToResponseDto {

		@Test
		void returnsUnixSecondsAsTAndServerTimeWithoutMilliseconds() throws Exception {
			long before = Instant.now().getEpochSecond();

			MvcResult result = mockMvc.perform(get("/api/time").header(ApiHeaders.CUSTOMER_NO, "1"))
					.andExpect(status().isOk())
					.andReturn();

			long after = Instant.now().getEpochSecond();
			JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

			assertThat(root.has("success")).isFalse();
			assertThat(root.has("t")).isTrue();
			assertThat(root.has("serverTime")).isTrue();
			assertThat(root.get("t").isIntegralNumber()).isTrue();
			assertThat(root.get("serverTime").isIntegralNumber()).isTrue();

			long t = root.get("t").asLong();
			long serverTime = root.get("serverTime").asLong();

			assertThat(t).isEqualTo(serverTime);
			assertThat(t).isBetween(before, after);
			// Doc: UNIX time without milliseconds (seconds ≈ 1e9; millis ≈ 1e12)
			assertThat(t).isLessThan(10_000_000_000L);
		}
	}

}
