package com.task.chart.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private List<String> corsOrigins = new ArrayList<>();
	private Binance binance = new Binance();

	public List<String> getCorsOrigins() {
		return corsOrigins;
	}

	public void setCorsOrigins(List<String> corsOrigins) {
		this.corsOrigins = corsOrigins;
	}

	public Binance getBinance() {
		return binance;
	}

	public void setBinance(Binance binance) {
		this.binance = binance;
	}

	public static class Binance {
		private String restBaseUrl = "https://api.binance.com";
		private String wsUrl = "wss://stream.binance.com:9443/ws";

		public String getRestBaseUrl() {
			return restBaseUrl;
		}

		public void setRestBaseUrl(String restBaseUrl) {
			this.restBaseUrl = restBaseUrl;
		}

		public String getWsUrl() {
			return wsUrl;
		}

		public void setWsUrl(String wsUrl) {
			this.wsUrl = wsUrl;
		}
	}
}
