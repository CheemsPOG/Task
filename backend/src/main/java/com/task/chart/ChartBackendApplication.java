package com.task.chart;

import com.task.chart.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ChartBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChartBackendApplication.class, args);
	}

}
