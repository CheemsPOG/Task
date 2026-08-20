package com.task.chart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

	@Bean
	WebMvcConfigurer corsConfigurer(AppProperties properties) {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
						.allowedOrigins(properties.getCorsOrigins().toArray(String[]::new))
						.allowedMethods("GET", "OPTIONS")
						.allowedHeaders("*");
				registry.addMapping("/curpairs")
						.allowedOrigins(properties.getCorsOrigins().toArray(String[]::new))
						.allowedMethods("GET", "OPTIONS")
						.allowedHeaders("*");
			}
		};
	}
}
