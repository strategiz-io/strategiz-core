package io.strategiz.service.livestrategies.service;

import io.strategiz.business.strategy.execution.model.ExecutionResult;
import io.strategiz.business.strategy.execution.service.StrategyExecutionService;
import io.strategiz.client.yahoofinance.client.YahooFinanceClient;
import io.strategiz.data.strategy.entity.StrategyAlert;
import io.strategiz.data.strategy.entity.StrategyAlertHistory;
import io.strategiz.data.strategy.repository.CreateStrategyAlertHistoryRepository;
import io.strategiz.data.strategy.repository.ReadStrategyAlertRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyAlertRepository;
import com.google.cloud.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled service that monitors active alerts and detects trading signals.
 * Runs every 5 minutes to check strategies and send notifications.
 *
 * Flow:
 * 1. Fetch all ACTIVE alerts
 * 2. For each alert:
 *    - Get current market data from Yahoo Finance
 *    - Execute the strategy code
 *    - Parse signals (BUY, SELL, HOLD)
 *    - Create history entries
 *    - Send notifications
 *    - Update alert metadata
 */
@Component
@ConditionalOnProperty(name = "alert.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class AlertMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(AlertMonitorService.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Instant lastRunTime;
    private int lastAlertCount = 0;
    private int lastSignalCount = 0;

    private final ReadStrategyAlertRepository readAlertRepository;
    private final UpdateStrategyAlertRepository updateAlertRepository;
    private final CreateStrategyAlertHistoryRepository createHistoryRepository;
    private final StrategyExecutionService strategyExecutionService;
    private final YahooFinanceClient yahooFinanceClient;
    private final AlertNotificationService notificationService;

    @Autowired
    public AlertMonitorService(
            ReadStrategyAlertRepository readAlertRepository,
            UpdateStrategyAlertRepository updateAlertRepository,
            CreateStrategyAlertHistoryRepository createHistoryRepository,
            StrategyExecutionService strategyExecutionService,
            YahooFinanceClient yahooFinanceClient,
            AlertNotificationService notificationService) {
        this.readAlertRepository = readAlertRepository;
        this.updateAlertRepository = updateAlertRepository;
        this.createHistoryRepository = createHistoryRepository;
        this.strategyExecutionService = strategyExecutionService;
        this.yahooFinanceClient = yahooFinanceClient;
        this.notificationService = notificationService;
    }

    /**
     * Main monitoring job - runs every 5 minutes (300000ms)
     *
     * FREE tier: Every 5 minutes
     * STARTER tier: Every 5 minutes (same as free, but more alerts)
     * PRO tier: Every 1 minute (TODO: implement tier-based intervals)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 min interval, 1 min initial delay
    public void monitorAlerts() {
        // Prevent concurrent executions
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Alert monitoring job is already running, skipping this execution");
            return;
        }

        try {
            logger.info("========================================");
            logger.info("Starting Alert Monitoring Cycle");
            logger.info("Time: {}", Instant.now());
            logger.info("========================================");

            lastRunTime = Instant.now();
            int alertsProcessed = 0;
            int signalsDetected = 0;
            int errors = 0;

            // Fetch all ACTIVE alerts (across all users)
            List<StrategyAlert> activeAlerts = readAlertRepository.findAllActive();
            logger.info("Found {} active alerts to monitor", activeAlerts.size());

            // Process each alert
            for (StrategyAlert alert : activeAlerts) {
                try {
                    int signalsFound = processAlert(alert);
                    alertsProcessed++;
                    signalsDetected += signalsFound;

                    if (signalsFound > 0) {
                        logger.info("Alert {} triggered {} signal(s)", alert.getId(), signalsFound);
                    }

                } catch (Exception e) {
                    logger.error("Error processing alert {}: {}", alert.getId(), e.getMessage(), e);
                    errors++;

                    // Update alert error message
                    try {
                        updateAlertRepository.updateErrorMessage(alert.getId(), alert.getUserId(), e.getMessage());
                    } catch (Exception updateError) {
                        logger.error("Failed to update alert error message", updateError);
                    }
                }
            }

            // Store metrics for monitoring
            lastAlertCount = alertsProcessed;
            lastSignalCount = signalsDetected;

            logger.info("========================================");
            logger.info("Alert Monitoring Cycle COMPLETED");
            logger.info("Alerts processed: {}", alertsProcessed);
            logger.info("Signals detected: {}", signalsDetected);
            logger.info("Errors encountered: {}", errors);
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Fatal error in alert monitoring job", e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Process a single alert - fetch market data, execute strategy, detect signals
     *
     * @param alert The alert to process
     * @return Number of signals detected
     */
    private int processAlert(StrategyAlert alert) {
        logger.debug("Processing alert: {} (strategy: {})", alert.getAlertName(), alert.getStrategyId());

        int signalCount = 0;

        // For each symbol in the alert
        for (String symbol : alert.getSymbols()) {
            try {
                // 1. Get current market data from Yahoo Finance
                Map<String, Object> marketData = yahooFinanceClient.fetchQuote(symbol);
                Double currentPrice = extractPriceFromResponse(marketData);

                if (currentPrice == null) {
                    logger.warn("Could not extract price for symbol: {}", symbol);
                    continue;
                }

                logger.debug("Current price for {}: ${}", symbol, currentPrice);

                // 2. Execute strategy to check for signals
                ExecutionResult result = strategyExecutionService.executeStrategy(
                    alert.getStrategyId(),
                    alert.getProviderId(),
                    alert.getUserId()
                );

                if (!"SUCCESS".equals(result.getStatus())) {
                    logger.warn("Strategy execution failed for alert {}: {}",
                        alert.getId(), result.getMessage());
                    continue;
                }

                // 3. Process signals from execution result
                List<ExecutionResult.Signal> signals = result.getSignals();
                if (signals != null && !signals.isEmpty()) {
                    for (ExecutionResult.Signal signal : signals) {
                        // Only process BUY/SELL signals (skip HOLD)
                        if ("BUY".equals(signal.getType()) || "SELL".equals(signal.getType())) {
                            signalCount++;

                            // Create history entry
                            createHistoryEntry(alert, signal, symbol, currentPrice, result.getMetrics());

                            // Send notifications
                            notificationService.sendSignalNotification(alert, signal, symbol, currentPrice);

                            // Update alert metadata
                            updateAlertMetadata(alert.getId(), alert.getUserId(), signal.getType());
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("Error processing symbol {} for alert {}: {}",
                    symbol, alert.getId(), e.getMessage());
            }
        }

        return signalCount;
    }

    /**
     * Extract price from Yahoo Finance API response
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

            logger.warn("Could not find price in Yahoo Finance response format");
            return null;

        } catch (Exception e) {
            logger.error("Error extracting price from response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create history entry for detected signal
     */
    private void createHistoryEntry(
            StrategyAlert alert,
            ExecutionResult.Signal signal,
            String symbol,
            Double price,
            Map<String, Object> metrics) {

        try {
            StrategyAlertHistory history = new StrategyAlertHistory();
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
            logger.error("Failed to create history entry for alert {}: {}",
                alert.getId(), e.getMessage(), e);
        }
    }

    /**
     * Update alert metadata after signal detection
     */
    private void updateAlertMetadata(String alertId, String userId, String signalType) {
        try {
            // Increment trigger count and update last triggered timestamp
            updateAlertRepository.recordTrigger(alertId, userId);

            logger.debug("Updated metadata for alert {} (signal: {})", alertId, signalType);

        } catch (Exception e) {
            logger.error("Failed to update alert metadata for {}: {}", alertId, e.getMessage());
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
