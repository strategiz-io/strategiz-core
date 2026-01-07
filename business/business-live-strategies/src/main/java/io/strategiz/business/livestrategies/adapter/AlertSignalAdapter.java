package io.strategiz.business.livestrategies.adapter;

import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.UpdateAlertDeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes alert signals to notification channels (email, SMS, push, in-app).
 * Each notification channel is handled by a dedicated sender.
 */
@Component
public class AlertSignalAdapter implements SignalAdapter {

	private static final Logger log = LoggerFactory.getLogger(AlertSignalAdapter.class);

	private final ReadAlertDeploymentRepository readAlertDeploymentRepository;
	private final UpdateAlertDeploymentRepository updateAlertDeploymentRepository;
	private final List<NotificationSender> notificationSenders;

	public AlertSignalAdapter(
			ReadAlertDeploymentRepository readAlertDeploymentRepository,
			UpdateAlertDeploymentRepository updateAlertDeploymentRepository,
			List<NotificationSender> notificationSenders) {
		this.readAlertDeploymentRepository = readAlertDeploymentRepository;
		this.updateAlertDeploymentRepository = updateAlertDeploymentRepository;
		this.notificationSenders = notificationSenders != null ? notificationSenders : new ArrayList<>();
		log.info("AlertSignalAdapter initialized with {} notification senders", this.notificationSenders.size());
	}

	@Override
	public boolean canHandle(Signal signal) {
		return signal != null && signal.isAlertSignal();
	}

	@Override
	public SignalResult process(Signal signal) {
		if (!canHandle(signal)) {
			return SignalResult.skipped(signal.getDeploymentId(), "Not an alert signal");
		}

		// Skip non-actionable signals (HOLD)
		if (!signal.isActionable()) {
			return SignalResult.skipped(signal.getDeploymentId(), "Signal is HOLD, no notification needed");
		}

		String alertId = signal.getDeploymentId();
		log.debug("Processing alert signal: {} for deployment {}", signal.getType(), alertId);

		try {
			// Load the alert deployment to get notification channels
			AlertDeployment alert = readAlertDeploymentRepository.findById(alertId)
					.orElse(null);

			if (alert == null) {
				log.warn("Alert deployment not found: {}", alertId);
				return SignalResult.failure(alertId, "UNKNOWN", "Alert deployment not found", null);
			}

			// Check if alert is active
			if (!"ACTIVE".equals(alert.getStatus())) {
				return SignalResult.skipped(alertId, "Alert is not active: " + alert.getStatus());
			}

			// Check cooldown
			if (isInCooldown(alert, signal)) {
				return SignalResult.skipped(alertId, "Alert in cooldown period");
			}

			// Check daily limit
			if (alert.isDailyLimitReached()) {
				return SignalResult.skipped(alertId, "Daily alert limit reached");
			}

			// Send to all configured notification channels
			List<String> channels = alert.getNotificationChannels();
			if (channels == null || channels.isEmpty()) {
				channels = List.of("email", "in-app"); // Default channels
			}

			List<String> successChannels = new ArrayList<>();
			List<String> failedChannels = new ArrayList<>();

			for (String channel : channels) {
				try {
					sendNotification(alert, signal, channel);
					successChannels.add(channel);
				} catch (Exception e) {
					log.error("Failed to send {} notification for alert {}: {}",
							channel, alertId, e.getMessage());
					failedChannels.add(channel);
				}
			}

			// Update alert state after sending
			updateAlertState(alert, signal);

			if (failedChannels.isEmpty()) {
				return SignalResult.success(alertId, String.join(",", successChannels),
						String.format("Sent to %d channels", successChannels.size()));
			} else if (successChannels.isEmpty()) {
				return SignalResult.failure(alertId, String.join(",", failedChannels),
						"All notification channels failed", null);
			} else {
				return SignalResult.success(alertId, String.join(",", successChannels),
						String.format("Partial success: %d/%d channels",
								successChannels.size(), channels.size()));
			}

		} catch (Exception e) {
			log.error("Error processing alert signal for {}: {}", alertId, e.getMessage(), e);
			return SignalResult.failure(alertId, "UNKNOWN", e.getMessage(), e);
		}
	}

	/**
	 * Check if the alert is in cooldown period.
	 */
	private boolean isInCooldown(AlertDeployment alert, Signal signal) {
		// Check if same signal type for same symbol was sent recently
		if (alert.getLastTriggeredAt() == null) {
			return false;
		}

		// Check signal deduplication (same signal type for same symbol)
		if (signal.getType().name().equals(alert.getLastSignalType())
				&& signal.getSymbol().equals(alert.getLastSignalSymbol())) {

			long cooldownMs = alert.getEffectiveCooldownMinutes() * 60 * 1000L;
			long lastTriggeredMs = alert.getLastTriggeredAt().toDate().getTime();
			long nowMs = System.currentTimeMillis();

			if (nowMs - lastTriggeredMs < cooldownMs) {
				log.debug("Alert {} in cooldown, last triggered {}ms ago, cooldown {}ms",
						alert.getId(), nowMs - lastTriggeredMs, cooldownMs);
				return true;
			}
		}

		return false;
	}

	/**
	 * Send notification to a specific channel.
	 */
	private void sendNotification(AlertDeployment alert, Signal signal, String channel) {
		// Find the appropriate sender for this channel
		for (NotificationSender sender : notificationSenders) {
			if (sender.supports(channel)) {
				sender.send(alert, signal);
				return;
			}
		}

		// If no sender found, log and skip
		log.warn("No notification sender found for channel: {}", channel);
	}

	/**
	 * Update alert state after successful notification.
	 */
	private void updateAlertState(AlertDeployment alert, Signal signal) {
		String alertId = alert.getId();
		String userId = alert.getUserId();

		// Record the signal (updates lastSignalType, lastSignalSymbol, lastTriggeredAt)
		updateAlertDeploymentRepository.recordSignal(alertId, userId,
				signal.getType().name(), signal.getSymbol());

		// Increment daily trigger count
		updateAlertDeploymentRepository.incrementDailyTriggerCount(alertId, userId);

		// Reset consecutive errors on success
		updateAlertDeploymentRepository.resetConsecutiveErrors(alertId, userId);
	}

	/**
	 * Interface for notification channel senders.
	 */
	public interface NotificationSender {
		boolean supports(String channel);
		void send(AlertDeployment alert, Signal signal);
	}

}
