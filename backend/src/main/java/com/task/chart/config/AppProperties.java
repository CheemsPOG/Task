/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Application configuration bound from the {@code app} prefix in {@code application.yml}.
 *
 * <p>Holds CORS origins, JWT secret/TTLs (S-01 stand-in), and TradingView datafeed flags for doc 120
 * {@code GET /api/config}. {@link com.task.chart.security.JwtService},
 * {@link com.task.chart.security.RefreshTokenStore}, {@link WebConfig}, and
 * {@link com.task.chart.service.impl.ChartDataServiceImpl} read these beans. This is NOT Peach production
 * config, NOT the Python WS env, and NOT the widget {@code datafeed.ts} constants.
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
 *   <tr><td>1.1.0</td><td>2026/08/25</td><td>Task</td><td>Split access and refresh TTL</td></tr>
 *   <tr><td>1.1.1</td><td>2026/08/27</td><td>Task</td><td>Onboarding comments</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.1.1
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private List<String> corsOrigins = new ArrayList<>();
	@NestedConfigurationProperty
	private Jwt jwt = new Jwt();
	@NestedConfigurationProperty
	private TradingView tradingView = new TradingView();

	public List<String> getCorsOrigins() {
		return corsOrigins;
	}

	public void setCorsOrigins(List<String> corsOrigins) {
		this.corsOrigins = corsOrigins;
	}

	public Jwt getJwt() {
		return jwt;
	}

	public void setJwt(Jwt jwt) {
		this.jwt = jwt;
	}

	public TradingView getTradingView() {
		return tradingView;
	}

	public void setTradingView(TradingView tradingView) {
		this.tradingView = tradingView;
	}

	/**
	 * Local JWT stand-in for S-01 (demo only).
	 *
	 * <p>Secret and TTLs for the 1h access JWT and 1d refresh Redis key. Not Peach SSO settings.
	 */
	public static class Jwt {

		private String secret = "chart-local-demo-jwt-secret-key-at-least-256-bits-long";
		private long accessExpirationMs = 3_600_000L;
		private long refreshExpirationMs = 86_400_000L;

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		public long getAccessExpirationMs() {
			return accessExpirationMs;
		}

		public void setAccessExpirationMs(long accessExpirationMs) {
			this.accessExpirationMs = accessExpirationMs;
		}

		public long getRefreshExpirationMs() {
			return refreshExpirationMs;
		}

		public void setRefreshExpirationMs(long refreshExpirationMs) {
			this.refreshExpirationMs = refreshExpirationMs;
		}
	}

	/**
	 * External configuration for datafeed {@code onReady} (design doc 120).
	 *
	 * <p>{@link com.task.chart.service.impl.ChartDataServiceImpl#config()} copies these flags into
	 * JSON. Session strings {@code timeSummer}/{@code timeWinter} are chosen from {@code m_season}.
	 */
	public static class TradingView {

		private boolean supportsSearch = true;
		private boolean supportsMarks = true;
		private boolean supportsTimescaleMarks = true;
		private boolean supportsTime = true;
		private String exchanges = "CTFX";
		private String symbolsTypes = "FOREX";
		private String timezone = "Asia/Tokyo";
		private boolean hasIntraday = true;
		private String visiblePlotsSet = "ohlc";
		private boolean hasSeconds = true;
		private String timeSummer = "0700-3000:2|0600-3000:345|0600-2940:6";
		private String timeWinter = "0700-3100:2|0700-3100:345|0700-3040:6";
		private int searchDefaultLimit = 100;
		private int searchMaxLimit = 100;
		private List<String> supportedResolutions = new ArrayList<>(List.of(
				"1S", "1", "5", "15", "30", "60", "120", "240", "480", "1D", "1W", "1M"));
		private List<String> intradayMultipliers = new ArrayList<>(List.of(
				"1", "5", "15", "30", "60", "120", "240", "480"));

		public boolean isSupportsSearch() {
			return supportsSearch;
		}

		public void setSupportsSearch(boolean supportsSearch) {
			this.supportsSearch = supportsSearch;
		}

		public boolean isSupportsMarks() {
			return supportsMarks;
		}

		public void setSupportsMarks(boolean supportsMarks) {
			this.supportsMarks = supportsMarks;
		}

		public boolean isSupportsTimescaleMarks() {
			return supportsTimescaleMarks;
		}

		public void setSupportsTimescaleMarks(boolean supportsTimescaleMarks) {
			this.supportsTimescaleMarks = supportsTimescaleMarks;
		}

		public boolean isSupportsTime() {
			return supportsTime;
		}

		public void setSupportsTime(boolean supportsTime) {
			this.supportsTime = supportsTime;
		}

		public String getExchanges() {
			return exchanges;
		}

		public void setExchanges(String exchanges) {
			this.exchanges = exchanges;
		}

		public String getSymbolsTypes() {
			return symbolsTypes;
		}

		public void setSymbolsTypes(String symbolsTypes) {
			this.symbolsTypes = symbolsTypes;
		}

		public String getTimezone() {
			return timezone;
		}

		public void setTimezone(String timezone) {
			this.timezone = timezone;
		}

		public boolean isHasIntraday() {
			return hasIntraday;
		}

		public void setHasIntraday(boolean hasIntraday) {
			this.hasIntraday = hasIntraday;
		}

		public String getVisiblePlotsSet() {
			return visiblePlotsSet;
		}

		public void setVisiblePlotsSet(String visiblePlotsSet) {
			this.visiblePlotsSet = visiblePlotsSet;
		}

		public boolean isHasSeconds() {
			return hasSeconds;
		}

		public void setHasSeconds(boolean hasSeconds) {
			this.hasSeconds = hasSeconds;
		}

		public String getTimeSummer() {
			return timeSummer;
		}

		public void setTimeSummer(String timeSummer) {
			this.timeSummer = timeSummer;
		}

		public String getTimeWinter() {
			return timeWinter;
		}

		public void setTimeWinter(String timeWinter) {
			this.timeWinter = timeWinter;
		}

		public int getSearchDefaultLimit() {
			return searchDefaultLimit;
		}

		public void setSearchDefaultLimit(int searchDefaultLimit) {
			this.searchDefaultLimit = searchDefaultLimit;
		}

		public int getSearchMaxLimit() {
			return searchMaxLimit;
		}

		public void setSearchMaxLimit(int searchMaxLimit) {
			this.searchMaxLimit = searchMaxLimit;
		}

		public List<String> getSupportedResolutions() {
			return supportedResolutions;
		}

		public void setSupportedResolutions(List<String> supportedResolutions) {
			this.supportedResolutions = supportedResolutions;
		}

		public List<String> getIntradayMultipliers() {
			return intradayMultipliers;
		}

		public void setIntradayMultipliers(List<String> intradayMultipliers) {
			this.intradayMultipliers = intradayMultipliers;
		}
	}
}
