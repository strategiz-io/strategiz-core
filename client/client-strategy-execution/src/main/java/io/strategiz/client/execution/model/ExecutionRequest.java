package io.strategiz.client.execution.model;

import java.util.List;

/**
 * Request to execute a trading strategy
 */
public class ExecutionRequest {

	private String code;

	private String language;

	private String userId;

	private String strategyId;

	private Integer timeoutSeconds;

	private List<MarketDataBar> marketData;

	public ExecutionRequest() {
	}

	public ExecutionRequest(String code, String language, String userId, String strategyId, Integer timeoutSeconds,
			List<MarketDataBar> marketData) {
		this.code = code;
		this.language = language;
		this.userId = userId;
		this.strategyId = strategyId;
		this.timeoutSeconds = timeoutSeconds;
		this.marketData = marketData;
	}

	public static ExecutionRequestBuilder builder() {
		return new ExecutionRequestBuilder();
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public List<MarketDataBar> getMarketData() {
		return marketData;
	}

	public void setMarketData(List<MarketDataBar> marketData) {
		this.marketData = marketData;
	}

	public static class ExecutionRequestBuilder {

		private String code;

		private String language;

		private String userId;

		private String strategyId;

		private Integer timeoutSeconds;

		private List<MarketDataBar> marketData;

		public ExecutionRequestBuilder code(String code) {
			this.code = code;
			return this;
		}

		public ExecutionRequestBuilder language(String language) {
			this.language = language;
			return this;
		}

		public ExecutionRequestBuilder userId(String userId) {
			this.userId = userId;
			return this;
		}

		public ExecutionRequestBuilder strategyId(String strategyId) {
			this.strategyId = strategyId;
			return this;
		}

		public ExecutionRequestBuilder timeoutSeconds(Integer timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
			return this;
		}

		public ExecutionRequestBuilder marketData(List<MarketDataBar> marketData) {
			this.marketData = marketData;
			return this;
		}

		public ExecutionRequest build() {
			return new ExecutionRequest(code, language, userId, strategyId, timeoutSeconds, marketData);
		}

	}

}
