/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security for {@code /api/**}.
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
 *   <tr><td>1.1.0</td><td>2026/08/24</td><td>Task</td><td>Permit Swagger UI for mentor review</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/24</td><td>Task</td><td>Move to security package</td></tr>
 *   <tr><td>1.3.0</td><td>2026/08/24</td><td>Task</td><td>Require JWT on GET /curpairs</td></tr>
 *   <tr><td>1.4.0</td><td>2026/08/25</td><td>Task</td><td>Permit refresh + logout</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.4.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JsonUnauthorizedEntryPoint jsonUnauthorizedEntryPoint;

	/**
	 * Creates the config.
	 *
	 * @param jwtAuthenticationFilter Bearer filter
	 * @param jsonUnauthorizedEntryPoint 401 JSON writer
	 */
	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter,
			JsonUnauthorizedEntryPoint jsonUnauthorizedEntryPoint) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.jsonUnauthorizedEntryPoint = jsonUnauthorizedEntryPoint;
	}

	/**
	 * Disables Spring Boot's generated in-memory user; login uses {@code AuthService} + JWT.
	 *
	 * @return stub user details service
	 */
	@Bean
	UserDetailsService userDetailsService() {
		return username -> {
			throw new UsernameNotFoundException(username);
		};
	}

	/**
	 * API security filter chain.
	 *
	 * @param http HTTP security
	 * @return filter chain
	 * @throws Exception on misconfiguration
	 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(jsonUnauthorizedEntryPoint))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
						.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs",
								"/v3/api-docs/**")
						.permitAll()
						.requestMatchers("/api/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/curpairs").authenticated()
						.anyRequest().permitAll())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
