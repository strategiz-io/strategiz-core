package io.strategiz.service.mystrategies.model.response;

import io.strategiz.data.strategy.entity.BotLivePerformance;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.List;

/**
 * Simplified bot deployment information for nesting within
 * StrategyWithDeploymentsResponse. Contains essential deployment info without full
 * details.
 */
public class BotDeploymentDTO {

	@JsonProperty("id")
	private String id;

	@JsonProperty("strategyId")
	private String strategyId;

	@JsonProperty("botName")
	private String botName;

	@JsonProperty("symbols")
	private List<String> symbols;

	@JsonProperty("status")
	private String status; // ACTIVE, PAUSED, STOPPED, ERROR

	@JsonProperty("environment")
	private String environment; // PAPER, LIVE

	@JsonProperty("deployedAt")
	private Timestamp deployedAt;

	@JsonProperty("livePerformance")
	private BotLivePerformance livePerformance;

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getBotName() {
		return botName;
	}

	public void setBotName(String botName) {
		this.botName = botName;
	}

	public List<String> getSymbols() {
		return symbols;
	}

	public void setSymbols(List<String> symbols) {
		this.symbols = symbols;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public Timestamp getDeployedAt() {
		return deployedAt;
	}

	public void setDeployedAt(Timestamp deployedAt) {
		this.deployedAt = deployedAt;
	}

	public BotLivePerformance getLivePerformance() {
		return livePerformance;
	}

	public void setLivePerformance(BotLivePerformance livePerformance) {
		this.livePerformance = livePerformance;
	}

}
