package io.strategiz.service.mystrategies.controller;

import io.strategiz.data.preferences.entity.AlertNotificationPreferences;
import io.strategiz.data.preferences.repository.AlertNotificationPreferencesRepository;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentHistoryRepository;
import io.strategiz.data.strategy.repository.CreateAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.UpdateAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.DeleteAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.mystrategies.exception.LiveStrategiesErrorDetails;
import io.strategiz.service.mystrategies.service.AlertNotificationService;
import io.strategiz.service.mystrategies.model.request.CreateAlertRequest;
import io.strategiz.service.mystrategies.model.request.UpdateAlertStatusRequest;
import io.strategiz.service.mystrategies.model.response.AlertResponse;
import io.strategiz.service.mystrategies.model.response.AlertHistoryResponse;
import io.strategiz.service.mystrategies.model.response.MessageResponse;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.cloud.Timestamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing strategy alerts.
 * Implements all endpoints defined in the UX spec.
 */
@RestController
@RequestMapping("/v1/alerts")
@Tag(name = "Strategy Alerts", description = "Manage trading strategy alerts and notifications")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);

    private final ReadAlertDeploymentRepository readAlertRepository;
    private final CreateAlertDeploymentRepository createAlertRepository;
    private final UpdateAlertDeploymentRepository updateAlertRepository;
    private final DeleteAlertDeploymentRepository deleteAlertRepository;
    private final ReadAlertDeploymentHistoryRepository readHistoryRepository;
    private final ReadStrategyRepository readStrategyRepository;
    private final UpdateStrategyRepository updateStrategyRepository;
    private final AlertNotificationPreferencesRepository alertPreferencesRepository;
    private final UserRepository userRepository;
    private final AlertNotificationService alertNotificationService;
    private final SubscriptionService subscriptionService;

    @Autowired
    public AlertController(
            ReadAlertDeploymentRepository readAlertRepository,
            CreateAlertDeploymentRepository createAlertRepository,
            UpdateAlertDeploymentRepository updateAlertRepository,
            DeleteAlertDeploymentRepository deleteAlertRepository,
            ReadAlertDeploymentHistoryRepository readHistoryRepository,
            ReadStrategyRepository readStrategyRepository,
            UpdateStrategyRepository updateStrategyRepository,
            AlertNotificationPreferencesRepository alertPreferencesRepository,
            UserRepository userRepository,
            AlertNotificationService alertNotificationService,
            SubscriptionService subscriptionService) {
        this.readAlertRepository = readAlertRepository;
        this.createAlertRepository = createAlertRepository;
        this.updateAlertRepository = updateAlertRepository;
        this.deleteAlertRepository = deleteAlertRepository;
        this.readHistoryRepository = readHistoryRepository;
        this.readStrategyRepository = readStrategyRepository;
        this.updateStrategyRepository = updateStrategyRepository;
        this.alertPreferencesRepository = alertPreferencesRepository;
        this.userRepository = userRepository;
        this.alertNotificationService = alertNotificationService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * GET /v1/alerts - List all user's alerts
     * Used by Live Strategies screen to display alert cards
     */
    @RequireAuth
    @GetMapping
    @Operation(summary = "Get all alerts", description = "Retrieve all alerts for the authenticated user")
    public ResponseEntity<List<AlertResponse>> getAllAlerts(@AuthUser String userId) {
        logger.info("Fetching all alerts for user: {}", userId);

        try {
            List<AlertDeployment> alerts = readAlertRepository.findByUserId(userId);
            List<AlertResponse> responses = alerts.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch alerts for user: {}", userId, e);
            throw new StrategizException(LiveStrategiesErrorDetails.ALERT_FETCH_FAILED, "service-my-strategies", e, new Object[0]);
        }
    }

    /**
     * POST /v1/alerts - Deploy new alert
     * Called from "Deploy Alert" dialog in Labs screen
     */
    @RequireAuth
    @PostMapping
    @Operation(summary = "Deploy new alert", description = "Create and deploy a new strategy alert")
    public ResponseEntity<MessageResponse> createAlert(
            @Valid @RequestBody CreateAlertRequest request,
            @AuthUser String userId) {

        logger.info("Creating alert '{}' for user: {}", request.getAlertName(), userId);
        logger.debug("Alert request details - strategyId: {}, symbols: {}, channels: {}, providerId: {}, exchange: {}, useDefaultContact: {}",
                request.getStrategyId(), request.getSymbols(), request.getNotificationChannels(),
                request.getProviderId(), request.getExchange(), request.getUseDefaultContact());

        try {
            // Validate strategy exists and belongs to user
            logger.debug("Fetching strategy with ID: {}", request.getStrategyId());
            Optional<Strategy> strategyOpt = readStrategyRepository.findById(request.getStrategyId());
            if (strategyOpt.isEmpty()) {
                logger.warn("Strategy not found: {}", request.getStrategyId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Strategy not found or access denied"));
            }

            Strategy strategy = strategyOpt.get();
            logger.debug("Strategy found - owner: {}, name: {}", strategy.getOwnerId(), strategy.getName());

            if (!userId.equals(strategy.getOwnerId())) {
                logger.warn("User {} does not own strategy {}", userId, request.getStrategyId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Strategy not found or access denied"));
            }

            // Check subscription tier limits (SCOUT: 3, TRADER: 10, STRATEGIST: unlimited)
            logger.debug("Checking alert limit for user: {}", userId);
            int activeCount = readAlertRepository.countActiveByUserId(userId);
            logger.debug("User has {} active alerts", activeCount);
            validateAlertLimit(userId, activeCount);

            // Resolve contact info for notifications
            logger.debug("Resolving notification contact info, useDefaultContact: {}", request.getUseDefaultContact());
            String notificationEmail = null;
            String notificationPhone = null;

            if (Boolean.FALSE.equals(request.getUseDefaultContact())) {
                // Use override values from request
                notificationEmail = request.getEmailOverride();
                notificationPhone = request.getPhoneOverride();
            } else {
                // Use default from user preferences or profile
                AlertNotificationPreferences prefs = alertPreferencesRepository.getByUserId(userId);

                // Email: prefer alert-specific email, fall back to profile email
                if (prefs.getEmailForAlerts() != null && !prefs.getEmailForAlerts().isEmpty()) {
                    notificationEmail = prefs.getEmailForAlerts();
                } else {
                    // Fall back to user profile email
                    Optional<UserEntity> user = userRepository.findById(userId);
                    if (user.isPresent() && user.get().getProfile() != null) {
                        notificationEmail = user.get().getProfile().getEmail();
                    }
                }

                // Phone: from preferences
                notificationPhone = prefs.getPhoneNumber();
            }

            // Get subscription tier from user profile
            String subscriptionTier = "FREE";
            Optional<UserEntity> user = userRepository.findById(userId);
            if (user.isPresent() && user.get().getProfile() != null) {
                String tier = user.get().getProfile().getSubscriptionTier();
                if (tier != null) {
                    subscriptionTier = tier.toUpperCase();
                }
            }

            // Create alert entity
            AlertDeployment alert = new AlertDeployment();
            alert.setStrategyId(request.getStrategyId());
            alert.setUserId(userId);
            alert.setAlertName(request.getAlertName());
            alert.setSymbols(request.getSymbols());
            alert.setProviderId(request.getProviderId());
            alert.setExchange(request.getExchange());
            alert.setNotificationChannels(request.getNotificationChannels());

            // If no channels specified, use defaults from preferences or ["email", "in-app"]
            if (alert.getNotificationChannels() == null || alert.getNotificationChannels().isEmpty()) {
                AlertNotificationPreferences prefs = alertPreferencesRepository.getByUserId(userId);
                if (prefs != null && prefs.getEnabledChannels() != null && !prefs.getEnabledChannels().isEmpty()) {
                    alert.setNotificationChannels(new ArrayList<>(prefs.getEnabledChannels()));
                } else {
                    alert.setNotificationChannels(List.of("email", "in-app"));
                }
                logger.info("Using default notification channels for alert {}: {}",
                            alert.getAlertName(), alert.getNotificationChannels());
            }

            // Log warning if push requested (coming soon feature)
            if (alert.getNotificationChannels().contains("push")) {
                logger.warn("Push notifications requested but not yet available - will be ignored");
            }

            alert.setNotificationEmail(notificationEmail);
            alert.setNotificationPhone(notificationPhone);
            alert.setStatus("ACTIVE");
            alert.setTriggerCount(0);
            alert.setSubscriptionTier(subscriptionTier);

            // Save alert
            AlertDeployment created = createAlertRepository.createWithUserId(alert, userId);

            // Update strategy deployment status
            updateStrategyRepository.updateDeploymentStatus(
                    request.getStrategyId(),
                    userId,
                    "ALERT",
                    created.getId()
            );

            MessageResponse response = new MessageResponse(
                    created.getId(),
                    "Alert deployed successfully! Monitoring " + String.join(", ", request.getSymbols())
            );
            response.setStatus("ACTIVE");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create alert", e);
            throw new StrategizException(LiveStrategiesErrorDetails.ALERT_CREATE_FAILED, "service-my-strategies", e, new Object[0]);
        }
    }

    /**
     * PATCH /v1/alerts/{id}/status - Update alert status
     * Used by pause/resume buttons on alert cards
     */
    @RequireAuth
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update alert status", description = "Pause, resume, or stop an alert")
    public ResponseEntity<MessageResponse> updateAlertStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateAlertStatusRequest request,
            @AuthUser String userId) {

        logger.info("Updating alert {} status to {} for user: {}", id, request.getStatus(), userId);

        try {
            boolean updated = updateAlertRepository.updateStatus(id, userId, request.getStatus());

            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Alert not found or access denied"));
            }

            String message = switch (request.getStatus()) {
                case "PAUSED" -> "Alert paused successfully";
                case "ACTIVE" -> "Alert resumed successfully";
                case "STOPPED" -> "Alert stopped successfully";
                default -> "Alert status updated";
            };

            MessageResponse response = new MessageResponse(message);
            response.setStatus(request.getStatus());

            return ResponseEntity.ok(response);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to update alert status", e);
            throw new StrategizException(LiveStrategiesErrorDetails.ALERT_UPDATE_FAILED, "service-my-strategies", e, new Object[0]);
        }
    }

    /**
     * GET /v1/alerts/{id}/history - Get alert trigger history
     * Used by "View History" side panel
     */
    @RequireAuth
    @GetMapping("/{id}/history")
    @Operation(summary = "Get alert history", description = "Retrieve trigger history for an alert")
    public ResponseEntity<AlertHistoryResponse> getAlertHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String signal,
            @AuthUser String userId) {

        logger.info("Fetching history for alert {} (user: {})", id, userId);

        try {
            // Verify alert belongs to user
            Optional<AlertDeployment> alert = readAlertRepository.findById(id);
            if (alert.isEmpty() || !userId.equals(alert.get().getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Fetch history entries with filters applied
            List<AlertDeploymentHistory> history;

            if (signal != null && !signal.isEmpty()) {
                // Filter by signal type (BUY, SELL, HOLD)
                history = readHistoryRepository.findBySignal(userId, signal.toUpperCase());
                // Further filter to this specific alert
                history = history.stream()
                    .filter(h -> id.equals(h.getAlertId()))
                    .collect(Collectors.toList());
            } else if (days != null && days > 0) {
                // Filter by time range (last N days)
                Instant startTime = Instant.now().minus(days, ChronoUnit.DAYS);
                Timestamp startTimestamp = Timestamp.ofTimeSecondsAndNanos(
                    startTime.getEpochSecond(),
                    startTime.getNano()
                );
                Timestamp endTimestamp = Timestamp.now();

                history = readHistoryRepository.findByTimeRange(userId, startTimestamp, endTimestamp);
                // Further filter to this specific alert
                history = history.stream()
                    .filter(h -> id.equals(h.getAlertId()))
                    .collect(Collectors.toList());
            } else {
                // No filters - fetch all for this alert
                history = readHistoryRepository.findByAlertId(id);
            }

            // Apply limit (always applied last, after other filters)
            if (history.size() > limit) {
                history = history.subList(0, limit);
            }

            AlertHistoryResponse response = new AlertHistoryResponse();
            response.setAlertId(id);
            response.setTotal(history.size());
            response.setHistory(history.stream()
                    .map(this::convertToHistoryEntry)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch alert history", e);
            throw new StrategizException(LiveStrategiesErrorDetails.ALERT_HISTORY_FETCH_FAILED, "service-my-strategies", e, new Object[0]);
        }
    }

    /**
     * DELETE /v1/alerts/{id} - Delete alert
     * Called from alert card menu → Delete
     */
    @RequireAuth
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete alert", description = "Stop and permanently delete an alert")
    public ResponseEntity<MessageResponse> deleteAlert(
            @PathVariable String id,
            @AuthUser String userId) {

        logger.info("Deleting alert {} for user: {}", id, userId);

        try {
            boolean deleted = deleteAlertRepository.delete(id, userId);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Alert not found or access denied"));
            }

            return ResponseEntity.ok(new MessageResponse("Alert deleted successfully"));
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete alert", e);
            throw new StrategizException(LiveStrategiesErrorDetails.ALERT_DELETE_FAILED, "service-my-strategies", e, new Object[0]);
        }
    }

    /**
     * POST /v1/alerts/{id}/test - Send test notification
     * Called from alert card menu → Test Alert
     */
    @RequireAuth
    @PostMapping("/{id}/test")
    @Operation(summary = "Send test notification", description = "Send a test notification for an alert")
    public ResponseEntity<MessageResponse> testAlert(
            @PathVariable String id,
            @AuthUser String userId) {

        logger.info("Sending test notification for alert {} (user: {})", id, userId);

        try {
            // Verify alert belongs to user
            Optional<AlertDeployment> alertOpt = readAlertRepository.findById(id);
            if (alertOpt.isEmpty() || !userId.equals(alertOpt.get().getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Alert not found or access denied"));
            }

            AlertDeployment alert = alertOpt.get();

            // Send test notification through all configured channels
            alertNotificationService.sendTestNotification(alert);

            // Build response with channel details
            String channels = String.join(", ", alert.getNotificationChannels());
            String message = String.format(
                "Test notification sent to %s. Check your %s for the test alert.",
                channels,
                alert.getNotificationChannels().contains("email") ? "inbox" : "notifications"
            );

            return ResponseEntity.ok(new MessageResponse(message));

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send test notification", e);
            throw new StrategizException(LiveStrategiesErrorDetails.NOTIFICATION_SEND_FAILED, "service-my-strategies", e, new Object[0]);
        }
    }

    // Helper methods

    private AlertResponse convertToResponse(AlertDeployment alert) {
        AlertResponse response = new AlertResponse();
        response.setId(alert.getId());
        response.setUserId(alert.getUserId());
        response.setStrategyId(alert.getStrategyId());
        response.setAlertName(alert.getAlertName());
        response.setSymbols(alert.getSymbols());
        response.setProviderId(alert.getProviderId());
        response.setExchange(alert.getExchange());
        response.setNotificationChannels(alert.getNotificationChannels());
        response.setStatus(alert.getStatus());
        response.setTriggerCount(alert.getTriggerCount());
        response.setLastTriggeredAt(alert.getLastTriggeredAt());
        response.setDeployedAt(alert.getCreatedDate());
        response.setSubscriptionTier(alert.getSubscriptionTier());
        response.setErrorMessage(alert.getErrorMessage());

        // Fetch strategy name
        try {
            Optional<Strategy> strategy = readStrategyRepository.findById(alert.getStrategyId());
            strategy.ifPresent(s -> response.setStrategyName(s.getName()));
        } catch (Exception e) {
            logger.warn("Failed to fetch strategy name for alert: {}", alert.getId(), e);
        }

        // Build last signal info (from most recent history entry)
        try {
            List<AlertDeploymentHistory> history = readHistoryRepository.findByAlertId(alert.getId());
            if (!history.isEmpty()) {
                AlertDeploymentHistory latest = history.get(0); // Assuming sorted by timestamp desc
                AlertResponse.LastSignalInfo lastSignal = new AlertResponse.LastSignalInfo();
                lastSignal.setSignal(latest.getSignal());
                lastSignal.setSymbol(latest.getSymbol());
                lastSignal.setPrice(latest.getPrice());
                response.setLastSignal(lastSignal);
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch last signal for alert: {}", alert.getId(), e);
        }

        return response;
    }

    private AlertHistoryResponse.HistoryEntry convertToHistoryEntry(AlertDeploymentHistory history) {
        AlertHistoryResponse.HistoryEntry entry = new AlertHistoryResponse.HistoryEntry();
        entry.setId(history.getId());
        entry.setSignal(history.getSignal());
        entry.setSymbol(history.getSymbol());
        entry.setPrice(history.getPrice());
        entry.setTimestamp(history.getTimestamp());
        entry.setNotificationSent(history.getNotificationSent());
        entry.setMetadata(history.getMetadata());
        return entry;
    }

    /**
     * Get maximum number of alerts allowed for a subscription tier.
     *
     * @param tier The subscription tier
     * @return Maximum alerts (0 = unlimited)
     */
    private int getMaxAlertsForTier(SubscriptionTier tier) {
        return switch (tier) {
            case TRIAL -> 3;        // Trial tier (limited)
            case EXPLORER -> 10;    // Entry paid tier
            case STRATEGIST -> 0;   // Mid tier (unlimited)
            case QUANT -> 0;        // Premium tier (unlimited)
        };
    }

    /**
     * Check if user can create more alerts based on their subscription tier.
     *
     * @param userId The user ID
     * @param currentActiveCount Current number of active alerts
     * @throws StrategizException if limit is exceeded
     */
    private void validateAlertLimit(String userId, int currentActiveCount) {
        SubscriptionTier tier = subscriptionService.getTier(userId);
        int maxAlerts = getMaxAlertsForTier(tier);

        // 0 means unlimited
        if (maxAlerts == 0) {
            return;
        }

        if (currentActiveCount >= maxAlerts) {
            String message = String.format(
                "Alert limit reached. Your %s tier allows %d alerts. Upgrade to create more alerts.",
                tier.getDisplayName(),
                maxAlerts
            );
            throw new StrategizException(
                LiveStrategiesErrorDetails.ALERT_LIMIT_EXCEEDED,
                "service-my-strategies",
                message
            );
        }
    }
}
