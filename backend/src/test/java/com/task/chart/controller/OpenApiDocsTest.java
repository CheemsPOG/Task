/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger UI / OpenAPI docs are public so a mentor can list and try APIs.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/24</td><td>Task</td><td>新規作成</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocsArePublicAndListControllers() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("CTFX Chart Backend"))
				.andExpect(jsonPath("$.paths['/api/auth/login'].post.summary").value("Login and get JWT"))
				.andExpect(jsonPath("$.paths['/api/config'].get.summary").value("120 Get datafeed configuration"))
				.andExpect(jsonPath("$.paths['/api/layouts'].post.summary").value("127 Register chart layout"))
				.andExpect(jsonPath("$.paths['/api/indicator-templates']").exists());
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isFound());
	}
}
