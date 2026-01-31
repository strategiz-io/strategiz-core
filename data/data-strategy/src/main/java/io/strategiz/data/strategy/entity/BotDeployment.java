package io.strategiz.data.strategy.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import io.strategiz.framework.resilience.circuitbreaker.CircuitBreakerState;
import io.strategiz.framework.resilience.circuitbreaker.CircuitState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.time.Instant;
import java.util.List;

/**
 * Entity representing a deployed strategy bot for automated trading. Links to a Strategy
 * entity and defines execution parameters.
 *
 * Implements CircuitBreakerState for fault tolerance - bots are automatically paused when
 * they experience too many consecutive failures. Bots have stricter circuit breaker
 * thresholds than alerts since real money is involved.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("botDeployments")
public class BotDeployment extends BaseEntity implements CircuitBreakerState {

	@JsonProperty("id")
	private String id;

	@JsonProperty("strategyId")
	private String strategyId;

	@JsonProperty("userId")
	private String userId;

	// Owner subscription model fields
	@JsonProperty("subscriptionId")
	private String subscriptionId; // Which subscription allows this deployment

	@JsonProperty("strategyOwnerId")
	private String strategyOwnerId; // Who owned strategy when bot was deployed

	@JsonProperty("strategyCreatorId")
	private String strategyCreatorId; // Original creator (for attribution)

	@JsonProperty("botName")
	private String botName;

	@JsonProperty("symbols")
	private List<String> symbols;

	@JsonProperty("providerId")
	private String providerId; // alpaca, coinbase, etc.

	@JsonProperty("exchange")
	private String exchange; // NYSE, NASDAQ, CRYPTO, etc.

	// Deployment environment
	@JsonProperty("environment")
	private String environment; // PAPER, LIVE

	// Simulated mode (until full trading execution is implemented)
	@JsonProperty("simulatedMode")
	private Boolean simulatedMode = true;

	// Risk management
	@JsonProperty("maxPositionSize")
	private Double maxPositionSize;

	@JsonProperty("stopLossPercent")
	private Double stopLossPercent;

	@JsonProperty("takeProfitPercent")
	private Double takeProfitPercent;

	@JsonProperty("maxDailyLoss")
	private Double maxDailyLoss;

	@JsonProperty("autoExecute")
	private Boolean autoExecute;

	// Status tracking
	@JsonProperty("status")
	private String status; // ACTIVE, PAUSED, ERROR, STOPPED

	@JsonProperty("lastCheckedAt")
	private Timestamp lastCheckedAt;

	@JsonProperty("lastExecutedAt")
	private Timestamp lastExecutedAt;

	@JsonProperty("totalTrades")
	private Integer totalTrades;

	@JsonProperty("profitableTrades")
	private Integer profitableTrades;

	@JsonProperty("totalPnL")
	private Double totalPnL;

	@JsonProperty("errorMessage")
	private String errorMessage;

	// Subscription tier (affects execution frequency)
	@JsonProperty("subscriptionTier")
	private String subscriptionTier; // FREE, STARTER, PRO

	// Circuit breaker fields (implements CircuitBreakerState)
	@JsonProperty("consecutiveErrors")
	private Integer consecutiveErrors; // Error count for circuit breaker

	@JsonProperty("consecutiveSuccesses")
	private Integer consecutiveSuccesses; // Success count for HALF_OPEN recovery

	@JsonProperty("circuitState")
	private CircuitState circuitState; // CLOSED, OPEN, HALF_OPEN

	@JsonProperty("circuitOpenedAt")
	private Timestamp circuitOpenedAt; // When circuit was opened (for reset timeout)

	@JsonProperty("maxConsecutiveErrors")
	private Integer maxConsecutiveErrors; // Threshold to pause (default: 3 for bots)

	@JsonProperty("dailyTradeCount")
	private Integer dailyTradeCount;

	@JsonProperty("dailyTradeLimit")
	private Integer dailyTradeLimit;

	@JsonProperty("lastDailyReset")
	private Timestamp lastDailyReset;

	// Live Performance Metrics
	@JsonProperty("livePerformance")
	private BotLivePerformance livePerformance; // Real-time trading performance metrics
												// (owner-only)

	// Constructors
	public BotDeployment() {
		super();
		this.totalTrades = 0;
		this.profitableTrades = 0;
		this.totalPnL = 0.0;
		this.status = "ACTIVE";
		this.environment = "PAPER";
		this.autoExecute = true;
		this.consecutiveErrors = 0;
		this.consecutiveSuccesses = 0;
		this.circuitState = CircuitState.CLOSED;
		this.maxConsecutiveErrors = 3; // Stricter for bots (real money involved)
		this.dailyTradeCount = 0;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	// Getters and Setters
	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	// Owner subscription model getters/setters
	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getStrategyOwnerId() {
		return strategyOwnerId;
	}

	public void setStrategyOwnerId(String strategyOwnerId) {
		this.strategyOwnerId = strategyOwnerId;
	}

	public String getStrategyCreatorId() {
		return strategyCreatorId;
	}

	public void setStrategyCreatorId(String strategyCreatorId) {
		this.strategyCreatorId = strategyCreatorId;
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

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public Boolean getSimulatedMode() {
		return simulatedMode;
	}

	public void setSimulatedMode(Boolean simulatedMode) {
		this.simulatedMode = simulatedMode;
	}

	public Double getMaxPositionSize() {
		return maxPositionSize;
	}

	public void setMaxPositionSize(Double maxPositionSize) {
		this.maxPositionSize = maxPositionSize;
	}

	public Double getStopLossPercent() {
		return stopLossPercent;
	}

	public void setStopLossPercent(Double stopLossPercent) {
		this.stopLossPercent = stopLossPercent;
	}

	public Double getTakeProfitPercent() {
		return takeProfitPercent;
	}

	public void setTakeProfitPercent(Double takeProfitPercent) {
		this.takeProfitPercent = takeProfitPercent;
	}

	public Double getMaxDailyLoss() {
		return maxDailyLoss;
	}

	public void setMaxDailyLoss(Double maxDailyLoss) {
		this.maxDailyLoss = maxDailyLoss;
	}

	public Boolean getAutoExecute() {
		return autoExecute;
	}

	public void setAutoExecute(Boolean autoExecute) {
		this.autoExecute = autoExecute;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Timestamp getLastCheckedAt() {
		return lastCheckedAt;
	}

	public void setLastCheckedAt(Timestamp lastCheckedAt) {
		this.lastCheckedAt = lastCheckedAt;
	}

	public Timestamp getLastExecutedAt() {
		return lastExecutedAt;
	}

	public void setLastExecutedAt(Timestamp lastExecutedAt) {
		this.lastExecutedAt = lastExecutedAt;
	}

	public Integer getTotalTrades() {
		return totalTrades;
	}

	public void setTotalTrades(Integer totalTrades) {
		this.totalTrades = totalTrades;
	}

	public Integer getProfitableTrades() {
		return profitableTrades;
	}

	public void setProfitableTrades(Integer profitableTrades) {
		this.profitableTrades = profitableTrades;
	}

	public Double getTotalPnL() {
		return totalPnL;
	}

	public void setTotalPnL(Double totalPnL) {
		this.totalPnL = totalPnL;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getSubscriptionTier() {
		return subscriptionTier;
	}

	public void setSubscriptionTier(String subscriptionTier) {
		this.subscriptionTier = subscriptionTier;
	}

	public Integer getConsecutiveErrors() {
		return consecutiveErrors;
	}

	public void setConsecutiveErrors(Integer consecutiveErrors) {
		this.consecutiveErrors = consecutiveErrors;
	}

	public Integer getMaxConsecutiveErrors() {
		return maxConsecutiveErrors;
	}

	public void setMaxConsecutiveErrors(Integer maxConsecutiveErrors) {
		this.maxConsecutiveErrors = maxConsecutiveErrors;
	}

	public Integer getDailyTradeCount() {
		return dailyTradeCount;
	}

	public void setDailyTradeCount(Integer dailyTradeCount) {
		this.dailyTradeCount = dailyTradeCount;
	}

	public Integer getDailyTradeLimit() {
		return dailyTradeLimit;
	}

	public void setDailyTradeLimit(Integer dailyTradeLimit) {
		this.dailyTradeLimit = dailyTradeLimit;
	}

	public Timestamp getLastDailyReset() {
		return lastDailyReset;
	}

	public void setLastDailyReset(Timestamp lastDailyReset) {
		this.lastDailyReset = lastDailyReset;
	}

	public BotLivePerformance getLivePerformance() {
		return livePerformance;
	}

	public void setLivePerformance(BotLivePerformance livePerformance) {
		this.livePerformance = livePerformance;
	}

	// CircuitBreakerState interface implementation

	@Override
	public Integer getConsecutiveFailures() {
		return consecutiveErrors;
	}

	@Override
	public void setConsecutiveFailures(Integer failures) {
		this.consecutiveErrors = failures;
	}

	@Override
	public Integer getConsecutiveSuccesses() {
		return consecutiveSuccesses;
	}

	@Override
	public void setConsecutiveSuccesses(Integer successes) {
		this.consecutiveSuccesses = successes;
	}

	@Override
	public CircuitState getCircuitState() {
		return circuitState;
	}

	@Override
	public void setCircuitState(CircuitState state) {
		this.circuitState = state;
	}

	@Override
	public Instant getCircuitOpenedAt() {
		return circuitOpenedAt != null ? Instant.ofEpochSecond(circuitOpenedAt.getSeconds(), circuitOpenedAt.getNanos())
				: null;
	}

	@Override
	public void setCircuitOpenedAt(Instant openedAt) {
		this.circuitOpenedAt = openedAt != null
				? Timestamp.ofTimeSecondsAndNanos(openedAt.getEpochSecond(), openedAt.getNano()) : null;
	}

	@Override
	public String getLastErrorMessage() {
		return errorMessage;
	}

	@Override
	public void setLastErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public Integer getFailureThreshold() {
		return maxConsecutiveErrors;
	}

	@Override
	public void setFailureThreshold(Integer threshold) {
		this.maxConsecutiveErrors = threshold;
	}

	// Convenience methods

	/**
	 * Check if this bot is in paper trading mode
	 */
	public boolean isPaperTrading() {
		return "PAPER".equals(environment);
	}

	/**
	 * Check if this bot is in live trading mode
	 */
	public boolean isLiveTrading() {
		return "LIVE".equals(environment);
	}

	/**
	 * Check if this bot is running in simulated mode (no real trades)
	 */
	public boolean isSimulated() {
		return Boolean.TRUE.equals(simulatedMode);
	}

	/**
	 * Calculate win rate
	 */
	public double getWinRate() {
		if (totalTrades == null || totalTrades == 0) {
			return 0.0;
		}
		int profitable = profitableTrades != null ? profitableTrades : 0;
		return (profitable * 100.0) / totalTrades;
	}

	/**
	 * Check if circuit breaker should trip
	 */
	public boolean shouldTripCircuitBreaker() {
		int threshold = maxConsecutiveErrors != null ? maxConsecutiveErrors : 3;
		int errors = consecutiveErrors != null ? consecutiveErrors : 0;
		return errors >= threshold;
	}

	/**
	 * Check if daily trade limit is reached
	 */
	public boolean isDailyLimitReached() {
		if (dailyTradeLimit == null) {
			return false;
		}
		int count = dailyTradeCount != null ? dailyTradeCount : 0;
		return count >= dailyTradeLimit;
	}

}
