/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI for local demo API review (JWT Authorize button).
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Tag order + JWT scheme for mentor review</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/24</td><td>Task</td><td>Add chart templates 136–139 tag</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER_JWT = "bearer-jwt";

	/**
	 * OpenAPI document: title, implemented APIs, and Bearer scheme for Try it out.
	 *
	 * @return OpenAPI bean
	 */
	@Bean
	OpenAPI chartOpenApi() {
		return new OpenAPI()
				.info(apiInfo())
				.tags(apiTags())
				.addSecurityItem(new SecurityRequirement().addList(BEARER_JWT))
				.components(new Components().addSecuritySchemes(BEARER_JWT, bearerScheme()));
	}

	private static Info apiInfo() {
		return new Info()
				.title("CTFX Chart Backend")
				.version("0.0.1-SNAPSHOT")
				.description(
						"Demo REST for TradingView Advanced Charts (design docs 120–139). "
								+ "Login first: POST /api/auth/login with demo/demo, then Authorize with the accessToken. "
								+ "Public (no token): GET /api/health, POST /api/auth/login, GET /curpairs, Swagger UI.");
	}

	private static List<Tag> apiTags() {
		return List.of(
				new Tag().name("Auth").description("Local JWT login (S-01 stand-in)"),
				new Tag().name("Datafeed (120–126)")
						.description("UDF: config, history, time, symbols, search, marks"),
				new Tag().name("Chart layouts (127–131)")
						.description("Register, update, get, list, delete"),
				new Tag().name("Indicator templates (132–135)")
						.description("List, upsert, get, delete study templates"),
				new Tag().name("Chart templates (136–139)")
						.description("List, upsert, get, delete chart templates"),
				new Tag().name("Currency pairs").description("Demo FX catalog"));
	}

	private static SecurityScheme bearerScheme() {
		return new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.description("Paste accessToken from POST /api/auth/login (no Bearer prefix).");
	}
}
