package io.strategiz.service.mystrategies.service;

import io.strategiz.business.strategy.execution.model.ExecutionResult;
import io.strategiz.business.strategy.execution.service.StrategyExecutionService;
import io.strategiz.client.yahoofinance.client.YahooFinanceClient;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import io.strategiz.data.strategy.entity.DeploymentType;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import io.strategiz.data.strategy.repository.CreateAlertDeploymentHistoryRepository;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.UpdateAlertDeploymentRepository;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import io.strategiz.service.base.BaseService;

/**
 * Scheduled service that monitors active alerts and detects trading signals.
 * Uses tier-based scheduling for different subscription levels.
 *
 * Tier-based evaluation frequency:
 * - PRO: Every 1 minute
 * - STARTER: Every 5 minutes
 * - FREE: Every 15 minutes
 *
 * Flow:
 * 1. Fetch active alerts by subscription tier
 * 2. For each alert:
 *    a. Check cooldown (skip if in cooldown period)
 *    b. Check daily rate limit (skip if exceeded)
 *    c. Check signal deduplication (skip if same signal)
 *    d. Get current market data
 *    e. Execute the strategy code
 *    f. Process signals (BUY, SELL)
 *    g. Send notifications (for ALERT deployment type)
 *    h. Update alert metadata and tracking
 *    i. Handle errors with circuit breaker
 */
@Component
@ConditionalOnProperty(name = "alert.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class AlertMonitorService extends BaseService {

    @Override
    protected String getModuleName() {
        return "unknown";
    }
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Instant lastRunTime;
    private int lastAlertCount = 0;
    private int lastSignalCount = 0;

    private final ReadAlertDeploymentRepository readAlertRepository;
    private final UpdateAlertDeploymentRepository updateAlertRepository;
    private final CreateAlertDeploymentHistoryRepository createHistoryRepository;
    private final StrategyExecutionService strategyExecutionService;
    private final MarketDataRepository marketDataRepository;
    private final YahooFinanceClient yahooFinanceClient; // Fallback for symbols not in cache
    private final AlertNotificationService notificationService;

    @Autowired
    public AlertMonitorService(
            ReadAlertDeploymentRepository readAlertRepository,
            UpdateAlertDeploymentRepository updateAlertRepository,
            CreateAlertDeploymentHistoryRepository createHistoryRepository,
            StrategyExecutionService strategyExecutionService,
            MarketDataRepository marketDataRepository,
            @Autowired(required = false) YahooFinanceClient yahooFinanceClient,
            AlertNotificationService notificationService) {
        this.readAlertRepository = readAlertRepository;
        this.updateAlertRepository = updateAlertRepository;
        this.createHistoryRepository = createHistoryRepository;
        this.strategyExecutionService = strategyExecutionService;
        this.marketDataRepository = marketDataRepository;
        this.yahooFinanceClient = yahooFinanceClient;
        this.notificationService = notificationService;
    }

    // ========================
    // Tier-Based Scheduling
    // ========================

    /**
     * PRO tier monitoring - runs every 1 minute (60000ms)
     * Highest frequency for premium subscribers.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000) // 1 min interval, 30s initial delay
    public void monitorProTierAlerts() {
        monitorAlertsByTier("PRO", 1);
    }

    /**
     * STARTER tier monitoring - runs every 5 minutes (300000ms)
     * Mid-tier frequency for starter subscribers.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 min interval, 1 min initial delay
    public void monitorStarterTierAlerts() {
        monitorAlertsByTier("STARTER", 5);
    }

    /**
     * FREE tier monitoring - runs every 15 minutes (900000ms)
     * Basic frequency for free users.
     */
    @Scheduled(fixedDelay = 900000, initialDelay = 120000) // 15 min interval, 2 min initial delay
    public void monitorFreeTierAlerts() {
        monitorAlertsByTier("FREE", 15);
    }

    /**
     * Legacy method for backward compatibility - monitors all alerts.
     * @deprecated Use tier-specific methods instead
     */
    @Deprecated
    public void monitorAlerts() {
        // Process all tiers
        monitorAlertsByTier("PRO", 1);
        monitorAlertsByTier("STARTER", 5);
        monitorAlertsByTier("FREE", 15);
    }

    /**
     * Monitor alerts for a specific subscription tier.
     *
     * @param tier The subscription tier (PRO, STARTER, FREE)
     * @param frequencyMinutes The evaluation frequency in minutes
     */
    private void monitorAlertsByTier(String tier, int frequencyMinutes) {
        // Use tier-specific lock to allow parallel tier processing
        // For simplicity, we use a global lock here - can be optimized later
        if (!isRunning.compareAndSet(false, true)) {
            log.debug("Alert monitoring already running, skipping {} tier", tier);
            return;
        }

        try {
            log.info("======== {} Tier Alert Monitoring ========", tier);
            log.info("Time: {} | Frequency: {} min", Instant.now(), frequencyMinutes);

            lastRunTime = Instant.now();
            int alertsProcessed = 0;
            int signalsDetected = 0;
            int skippedCooldown = 0;
            int skippedRateLimit = 0;
            int skippedDedup = 0;
            int errors = 0;

            // Fetch active alerts for this tier (ALERT deployment type only)
            List<AlertDeployment> tierAlerts = readAlertRepository.findActiveAlertsByTier(tier);
            log.info("Found {} active {} alerts to monitor", tierAlerts.size(), tier);

            // Process each alert
            for (AlertDeployment alert : tierAlerts) {
                try {
                    // Skip BOT deployment types (future feature)
                    if (!alert.isAlertDeployment()) {
                        log.debug("Skipping non-alert deployment: {}", alert.getId());
                        continue;
                    }

                    // Reset daily count if needed
                    resetDailyCountIfNeeded(alert);

                    // Check rate limit
                    if (alert.isDailyLimitReached()) {
                        log.debug("Alert {} has reached daily limit", alert.getId());
                        skippedRateLimit++;
                        continue;
                    }

                    ProcessResult result = processAlertWithChecks(alert);
                    alertsProcessed++;

                    switch (result.outcome) {
                        case SIGNAL_TRIGGERED -> signalsDetected += result.signalCount;
                        case SKIPPED_COOLDOWN -> skippedCooldown++;
                        case SKIPPED_DEDUP -> skippedDedup++;
                        case NO_SIGNAL -> {} // Normal, no action needed
                        case ERROR -> errors++;
                    }

                } catch (Exception e) {
                    log.error("Error processing alert {}: {}", alert.getId(), e.getMessage(), e);
                    errors++;
                    handleAlertError(alert, e);
                }
            }

            // Store metrics for monitoring
            lastAlertCount = alertsProcessed;
            lastSignalCount = signalsDetected;

            log.info("======== {} Tier COMPLETED ========", tier);
            log.info("Processed: {} | Signals: {} | Errors: {}", alertsProcessed, signalsDetected, errors);
            log.info("Skipped - Cooldown: {} | RateLimit: {} | Dedup: {}",
                    skippedCooldown, skippedRateLimit, skippedDedup);

        } catch (Exception e) {
            log.error("Fatal error in {} tier monitoring", tier, e);
        } finally {
            isRunning.set(false);
        }
    }

    // ========================
    // Process Result Tracking
    // ========================

    private enum ProcessOutcome {
        SIGNAL_TRIGGERED,
        NO_SIGNAL,
        SKIPPED_COOLDOWN,
        SKIPPED_DEDUP,
        ERROR
    }

    private record ProcessResult(ProcessOutcome outcome, int signalCount) {}

    /**
     * Process alert with cooldown and deduplication checks.
     */
    private ProcessResult processAlertWithChecks(AlertDeployment alert) {
        log.debug("Processing alert: {} (strategy: {})", alert.getAlertName(), alert.getStrategyId());

        int signalCount = 0;

        // For each symbol in the alert
        for (String symbol : alert.getSymbols()) {
            try {
                // Check cooldown for this symbol
                if (isInCooldown(alert, symbol)) {
                    log.debug("Alert {} symbol {} in cooldown, skipping", alert.getId(), symbol);
                    return new ProcessResult(ProcessOutcome.SKIPPED_COOLDOWN, 0);
                }

                // 1. Get current market data from cache (populated by batch job)
                Double currentPrice = getLatestPriceFromCache(symbol);

                if (currentPrice == null) {
                    log.warn("No cached price data for symbol: {} - batch job may not have collected it yet", symbol);
                    continue;
                }

                log.debug("Cached price for {}: ${}", symbol, currentPrice);

                // Update last checked timestamp
                updateAlertRepository.updateLastCheckedAt(alert.getId(), alert.getUserId(), Timestamp.now());

                // 2. Execute strategy to check for signals
                ExecutionResult result = strategyExecutionService.executeStrategy(
                    alert.getStrategyId(),
                    alert.getProviderId(),
                    alert.getUserId()
                );

                if (!"SUCCESS".equals(result.getStatus())) {
                    log.warn("Strategy execution failed for alert {}: {}",
                        alert.getId(), result.getMessage());
                    continue;
                }

                // Reset consecutive errors on successful execution
                if (alert.getConsecutiveErrors() != null && alert.getConsecutiveErrors() > 0) {
                    updateAlertRepository.resetConsecutiveErrors(alert.getId(), alert.getUserId());
                }

                // 3. Process signals from execution result
                List<ExecutionResult.Signal> signals = result.getSignals();
                if (signals != null && !signals.isEmpty()) {
                    for (ExecutionResult.Signal signal : signals) {
                        // Only process BUY/SELL signals (skip HOLD)
                        if ("BUY".equals(signal.getType()) || "SELL".equals(signal.getType())) {

                            // Check deduplication: skip if same signal type as last
                            if (isDuplicateSignal(alert, signal.getType(), symbol)) {
                                log.debug("Duplicate signal {} for {} on alert {}, skipping",
                                        signal.getType(), symbol, alert.getId());
                                return new ProcessResult(ProcessOutcome.SKIPPED_DEDUP, 0);
                            }

                            signalCount++;

                            // Create history entry
                            createHistoryEntry(alert, signal, symbol, currentPrice, result.getMetrics());

                            // Send notifications (for ALERT deployment type)
                            if (alert.isAlertDeployment()) {
                                notificationService.sendSignalNotification(alert, signal, symbol, currentPrice);
                            }

                            // Record signal for cooldown and deduplication
                            updateAlertRepository.recordSignal(
                                    alert.getId(),
                                    alert.getUserId(),
                                    signal.getType(),
                                    symbol
                            );

                            log.info("Alert {} triggered {} signal for {} at ${}",
                                    alert.getId(), signal.getType(), symbol, currentPrice);
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Error processing symbol {} for alert {}: {}",
                    symbol, alert.getId(), e.getMessage());
                return new ProcessResult(ProcessOutcome.ERROR, 0);
            }
        }

        return new ProcessResult(
                signalCount > 0 ? ProcessOutcome.SIGNAL_TRIGGERED : ProcessOutcome.NO_SIGNAL,
                signalCount
        );
    }

    // ========================
    // Cooldown & Deduplication
    // ========================

    /**
     * Check if alert is in cooldown period.
     * Cooldown is per-symbol and based on subscription tier.
     */
    private boolean isInCooldown(AlertDeployment alert, String symbol) {
        Timestamp lastTriggered = alert.getLastTriggeredAt();
        if (lastTriggered == null) {
            return false; // Never triggered, not in cooldown
        }

        // Check if last trigger was for a different symbol
        String lastSymbol = alert.getLastSignalSymbol();
        if (lastSymbol != null && !lastSymbol.equals(symbol)) {
            return false; // Different symbol, not in cooldown
        }

        // Calculate cooldown based on tier
        int cooldownMinutes = alert.getEffectiveCooldownMinutes();
        Instant cooldownEnd = lastTriggered.toDate().toInstant().plus(cooldownMinutes, ChronoUnit.MINUTES);

        return Instant.now().isBefore(cooldownEnd);
    }

    /**
     * Check if this is a duplicate signal (same signal type for same symbol).
     * This prevents spamming the same BUY/SELL signal repeatedly.
     */
    private boolean isDuplicateSignal(AlertDeployment alert, String signalType, String symbol) {
        String lastSignalType = alert.getLastSignalType();
        String lastSignalSymbol = alert.getLastSignalSymbol();

        // If same signal type and same symbol, it's a duplicate
        // User should only be notified when signal CHANGES (e.g., BUY -> SELL)
        return signalType.equals(lastSignalType) &&
               symbol.equals(lastSignalSymbol);
    }

    /**
     * Reset daily trigger count if it's a new day.
     */
    private void resetDailyCountIfNeeded(AlertDeployment alert) {
        Timestamp lastReset = alert.getLastDailyReset();
        if (lastReset == null) {
            // Never reset, do it now
            updateAlertRepository.resetDailyTriggerCount(alert.getId(), alert.getUserId());
            return;
        }

        // Check if last reset was on a different day
        LocalDate lastResetDate = lastReset.toDate().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (lastResetDate.isBefore(today)) {
            log.debug("Resetting daily trigger count for alert {} (last reset: {})",
                    alert.getId(), lastResetDate);
            updateAlertRepository.resetDailyTriggerCount(alert.getId(), alert.getUserId());
        }
    }

    // ========================
    // Error Handling
    // ========================

    /**
     * Handle alert processing error with circuit breaker logic.
     */
    private void handleAlertError(AlertDeployment alert, Exception e) {
        try {
            updateAlertRepository.incrementConsecutiveErrors(
                    alert.getId(),
                    alert.getUserId(),
                    e.getMessage()
            );

            // Check if circuit breaker tripped (status changed to ERROR)
            if (alert.shouldTripCircuitBreaker()) {
                log.warn("Circuit breaker tripped for alert {} after {} consecutive errors",
                        alert.getId(), alert.getConsecutiveErrors());
            }
        } catch (Exception updateError) {
            log.error("Failed to update alert error state", updateError);
        }
    }

    // ========================
    // Market Data Access
    // ========================

    /**
     * Get latest price from cached market data (populated by batch job).
     * Falls back to Yahoo Finance if cache miss and fallback is available.
     *
     * @param symbol The symbol to get price for
     * @return The latest close price, or null if not available
     */
    private Double getLatestPriceFromCache(String symbol) {
        try {
            // Try to get from cached market data
            Optional<MarketDataEntity> cachedData = marketDataRepository.findLatestBySymbol(symbol);

            if (cachedData.isPresent()) {
                MarketDataEntity data = cachedData.get();
                BigDecimal closePrice = data.getClose();

                if (closePrice != null) {
                    // Check data freshness (warn if older than 30 minutes for 1Min bars)
                    if (data.getTimestamp() != null) {
                        long ageMinutes = ChronoUnit.MINUTES.between(
                                Instant.ofEpochMilli(data.getTimestamp()),
                                Instant.now()
                        );
                        if (ageMinutes > 30) {
                            log.warn("Cached data for {} is {} minutes old (batch may be delayed)",
                                    symbol, ageMinutes);
                        }
                    }

                    log.debug("Using cached price for {}: ${} (source: {}, timeframe: {})",
                            symbol, closePrice, data.getDataSource(), data.getTimeframe());
                    return closePrice.doubleValue();
                }
            }

            // Cache miss - try Yahoo Finance fallback if available
            if (yahooFinanceClient != null) {
                log.info("Cache miss for {}, falling back to Yahoo Finance", symbol);
                Map<String, Object> response = yahooFinanceClient.fetchQuote(symbol);
                return extractPriceFromResponse(response);
            }

            log.warn("No cached data for {} and no fallback available", symbol);
            return null;

        } catch (Exception e) {
            log.error("Error getting price for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Extract price from Yahoo Finance API response (fallback only)
     */
    private Double extractPriceFromResponse(Map<String, Object> response) {
        try {
            // Try v10 API format first: quoteSummary.result[0].price.regularMarketPrice.raw
            if (response.containsKey("quoteSummary")) {
                Map<String, Object> quoteSummary = (Map<String, Object>) response.get("quoteSummary");
                if (quoteSummary != null && quoteSummary.containsKey("result")) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) quoteSummary.get("result");
                    if (results != null && !results.isEmpty()) {
                        Map<String, Object> price = (Map<String, Object>) results.get(0).get("price");
                        if (price != null && price.containsKey("regularMarketPrice")) {
                            Map<String, Object> marketPrice = (Map<String, Object>) price.get("regularMarketPrice");
                            if (marketPrice != null && marketPrice.containsKey("raw")) {
                                return ((Number) marketPrice.get("raw")).doubleValue();
                            }
                        }
                    }
                }
            }

            // Try v8 API format: quoteResponse.result[0].regularMarketPrice
            if (response.containsKey("quoteResponse")) {
                Map<String, Object> quoteResponse = (Map<String, Object>) response.get("quoteResponse");
                if (quoteResponse != null && quoteResponse.containsKey("result")) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) quoteResponse.get("result");
                    if (results != null && !results.isEmpty()) {
                        Object priceObj = results.get(0).get("regularMarketPrice");
                        if (priceObj != null) {
                            return ((Number) priceObj).doubleValue();
                        }
                    }
                }
            }

            log.warn("Could not find price in Yahoo Finance response format");
            return null;

        } catch (Exception e) {
            log.error("Error extracting price from response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create history entry for detected signal
     */
    private void createHistoryEntry(
            AlertDeployment alert,
            ExecutionResult.Signal signal,
            String symbol,
            Double price,
            Map<String, Object> metrics) {

        try {
            AlertDeploymentHistory history = new AlertDeploymentHistory();
            history.setAlertId(alert.getId());
            history.setUserId(alert.getUserId());
            history.setSignal(signal.getType());
            history.setSymbol(symbol);
            history.setPrice(price);
            history.setTimestamp(Timestamp.now());
            history.setNotificationSent(true); // Will be sent by notification service

            // Add strategy metrics (RSI, MACD, etc.) to metadata
            Map<String, Object> metadata = new HashMap<>();
            if (metrics != null) {
                metadata.putAll(metrics);
            }
            metadata.put("reason", signal.getReason());
            metadata.put("quantity", signal.getQuantity());
            history.setMetadata(metadata);

            createHistoryRepository.createWithUserId(history, alert.getUserId());

        } catch (Exception e) {
            log.error("Failed to create history entry for alert {}: {}",
                alert.getId(), e.getMessage(), e);
        }
    }

    /**
     * Get monitoring status for health checks
     */
    public MonitoringStatus getStatus() {
        return new MonitoringStatus(
            isRunning.get(),
            lastRunTime,
            lastAlertCount,
            lastSignalCount
        );
    }

    /**
     * Status object for monitoring
     */
    public static class MonitoringStatus {
        private final boolean running;
        private final Instant lastRun;
        private final int alertsProcessed;
        private final int signalsDetected;

        public MonitoringStatus(boolean running, Instant lastRun, int alertsProcessed, int signalsDetected) {
            this.running = running;
            this.lastRun = lastRun;
            this.alertsProcessed = alertsProcessed;
            this.signalsDetected = signalsDetected;
        }

        public boolean isRunning() { return running; }
        public Instant getLastRun() { return lastRun; }
        public int getAlertsProcessed() { return alertsProcessed; }
        public int getSignalsDetected() { return signalsDetected; }
    }
}
