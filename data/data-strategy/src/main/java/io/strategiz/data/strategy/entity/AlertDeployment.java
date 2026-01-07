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
 * Entity representing a deployed strategy alert.
 * Links to a Strategy entity and defines how/when to alert the user.
 *
 * Implements CircuitBreakerState for fault tolerance - alerts are automatically
 * paused when they experience too many consecutive failures.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("alertDeployments")
public class AlertDeployment extends BaseEntity implements CircuitBreakerState {

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
    private String strategyOwnerId; // Who owned strategy when alert was deployed

    @JsonProperty("strategyCreatorId")
    private String strategyCreatorId; // Original creator (for attribution)

    @JsonProperty("alertName")
    private String alertName;

    @JsonProperty("symbols")
    private List<String> symbols;

    @JsonProperty("providerId")
    private String providerId; // coinbase, schwab, etc.

    @JsonProperty("exchange")
    private String exchange; // NYSE, NASDAQ, CRYPTO, etc.

    @JsonProperty("notificationChannels")
    private List<String> notificationChannels; // email, push, in-app, sms
    // NOTE: "push" channel is COMING SOON - requires mobile app with FCM registration
    // Default channels: ["email", "in-app"]

    // Resolved contact info for notifications
    @JsonProperty("notificationEmail")
    private String notificationEmail; // Email address for email notifications

    @JsonProperty("notificationPhone")
    private String notificationPhone; // Phone number for SMS notifications

    @JsonProperty("status")
    private String status; // ACTIVE, PAUSED, ERROR, STOPPED

    @JsonProperty("lastCheckedAt")
    private Timestamp lastCheckedAt;

    @JsonProperty("lastTriggeredAt")
    private Timestamp lastTriggeredAt;

    @JsonProperty("triggerCount")
    private Integer triggerCount;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("subscriptionTier")
    private String subscriptionTier; // FREE, STARTER, PRO

    // Cooldown and deduplication fields
    @JsonProperty("cooldownMinutes")
    private Integer cooldownMinutes; // Min time between alerts (default: 15 for FREE, 5 for STARTER, 1 for PRO)

    @JsonProperty("lastSignalType")
    private String lastSignalType; // Last signal type for deduplication (BUY, SELL, HOLD)

    @JsonProperty("lastSignalSymbol")
    private String lastSignalSymbol; // Last symbol that triggered

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
    private Integer maxConsecutiveErrors; // Threshold to pause (default: 5)

    // Rate limiting
    @JsonProperty("evaluationFrequencyMinutes")
    private Integer evaluationFrequencyMinutes; // How often to evaluate (tier-based)

    @JsonProperty("dailyTriggerCount")
    private Integer dailyTriggerCount; // Alerts triggered today

    @JsonProperty("dailyTriggerLimit")
    private Integer dailyTriggerLimit; // Max alerts per day (tier-based)

    @JsonProperty("lastDailyReset")
    private Timestamp lastDailyReset; // When daily count was reset

    // Constructors
    public AlertDeployment() {
        super();
        this.triggerCount = 0;
        this.status = "ACTIVE";
        this.consecutiveErrors = 0;
        this.consecutiveSuccesses = 0;
        this.circuitState = CircuitState.CLOSED;
        this.maxConsecutiveErrors = 5;
        this.dailyTriggerCount = 0;
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

    public String getAlertName() {
        return alertName;
    }

    public void setAlertName(String alertName) {
        this.alertName = alertName;
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

    public List<String> getNotificationChannels() {
        return notificationChannels;
    }

    public void setNotificationChannels(List<String> notificationChannels) {
        this.notificationChannels = notificationChannels;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public String getNotificationPhone() {
        return notificationPhone;
    }

    public void setNotificationPhone(String notificationPhone) {
        this.notificationPhone = notificationPhone;
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

    public Timestamp getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(Timestamp lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Integer getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
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

    public Integer getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(Integer cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public String getLastSignalType() {
        return lastSignalType;
    }

    public void setLastSignalType(String lastSignalType) {
        this.lastSignalType = lastSignalType;
    }

    public String getLastSignalSymbol() {
        return lastSignalSymbol;
    }

    public void setLastSignalSymbol(String lastSignalSymbol) {
        this.lastSignalSymbol = lastSignalSymbol;
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
        return circuitOpenedAt != null ? Instant.ofEpochSecond(circuitOpenedAt.getSeconds(), circuitOpenedAt.getNanos()) : null;
    }

    @Override
    public void setCircuitOpenedAt(Instant openedAt) {
        this.circuitOpenedAt = openedAt != null ? Timestamp.ofTimeSecondsAndNanos(openedAt.getEpochSecond(), openedAt.getNano()) : null;
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

    // Legacy getters/setters (for backward compatibility)

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

    public Integer getEvaluationFrequencyMinutes() {
        return evaluationFrequencyMinutes;
    }

    public void setEvaluationFrequencyMinutes(Integer evaluationFrequencyMinutes) {
        this.evaluationFrequencyMinutes = evaluationFrequencyMinutes;
    }

    public Integer getDailyTriggerCount() {
        return dailyTriggerCount;
    }

    public void setDailyTriggerCount(Integer dailyTriggerCount) {
        this.dailyTriggerCount = dailyTriggerCount;
    }

    public Integer getDailyTriggerLimit() {
        return dailyTriggerLimit;
    }

    public void setDailyTriggerLimit(Integer dailyTriggerLimit) {
        this.dailyTriggerLimit = dailyTriggerLimit;
    }

    public Timestamp getLastDailyReset() {
        return lastDailyReset;
    }

    public void setLastDailyReset(Timestamp lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }

    // Convenience methods

    /**
     * Check if this is an ALERT deployment type
     * AlertDeployment is always an alert deployment
     */
    public boolean isAlertDeployment() {
        return true;
    }

    /**
     * Check if this is a BOT deployment type
     * AlertDeployment is never a bot deployment
     */
    public boolean isBotDeployment() {
        return false;
    }

    /**
     * Get effective cooldown minutes based on subscription tier
     */
    public int getEffectiveCooldownMinutes() {
        if (cooldownMinutes != null) {
            return cooldownMinutes;
        }
        // Default based on tier
        return switch (subscriptionTier != null ? subscriptionTier : "FREE") {
            case "PRO" -> 1;
            case "STARTER" -> 5;
            default -> 15; // FREE
        };
    }

    /**
     * Get effective evaluation frequency based on subscription tier
     */
    public int getEffectiveEvaluationFrequencyMinutes() {
        if (evaluationFrequencyMinutes != null) {
            return evaluationFrequencyMinutes;
        }
        // Default based on tier
        return switch (subscriptionTier != null ? subscriptionTier : "FREE") {
            case "PRO" -> 1;
            case "STARTER" -> 5;
            default -> 15; // FREE
        };
    }

    /**
     * Check if circuit breaker should trip (too many consecutive errors)
     */
    public boolean shouldTripCircuitBreaker() {
        int threshold = maxConsecutiveErrors != null ? maxConsecutiveErrors : 5;
        int errors = consecutiveErrors != null ? consecutiveErrors : 0;
        return errors >= threshold;
    }

    /**
     * Check if daily limit is reached
     */
    public boolean isDailyLimitReached() {
        if (dailyTriggerLimit == null) {
            return false; // No limit
        }
        int count = dailyTriggerCount != null ? dailyTriggerCount : 0;
        return count >= dailyTriggerLimit;
    }
}
