/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart;

import com.task.chart.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the chart REST backend.
 *
 * <p>{@code @EnableScheduling} starts {@code TickIngestWorker} (the only live
 * OHLC writer). {@code ChartCacheWriter} seeds warehouse/Redis at boot. HTTP
 * lives in {@code controller} (docs 120–139). Python is a separate process that
 * only relays Redis {@code peach:quotes} (ticks) and {@code peach:bars}
 * (forming candles). If scheduling silently stops app-wide, check
 * {@code @EnableScheduling} here first.
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
 *   <tr><td>1.1.0</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.0
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableScheduling
public class ChartBackendApplication {

	/**
	 * Starts the REST backend, boot bar seed, and scheduled tick ingest.
	 *
	 * @param args command line arguments
	 */
	public static void main(String[] args) {

		SpringApplication.run(ChartBackendApplication.class, args);
	}

}
