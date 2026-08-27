/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for local frontend origins.
 *
 * <p>Allows {@code app.cors-origins} (Vite :5173 and :3000) on {@code /api/**} and {@code GET /curpairs}.
 * The chart UI normally uses the Vite proxy so cookies stay same-origin; this bean is for direct hits
 * to Java :8080. Spring MVC loads it at boot. This is NOT {@link com.task.chart.security.SecurityConfig}
 * (JWT matchers), NOT the Python WS, and NOT the widget.
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
 *   <tr><td>1.0.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.0.1
 */
@Configuration
public class WebConfig {

	/**
	 * CORS mappings for {@code /api/**} and {@code GET /curpairs}.
	 *
	 * @param properties {@code app.cors-origins}
	 * @return MVC configurer
	 */
	@Bean
	WebMvcConfigurer corsConfigurer(AppProperties properties) {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
						.allowedOrigins(properties.getCorsOrigins().toArray(String[]::new))
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
						.allowedHeaders("*");

				// Same origins as /api; path is outside /api/** so it needs its own mapping.
				registry.addMapping("/curpairs")
						.allowedOrigins(properties.getCorsOrigins().toArray(String[]::new))
						.allowedMethods("GET", "OPTIONS")
						.allowedHeaders("*");
			}
		};
	}
}
