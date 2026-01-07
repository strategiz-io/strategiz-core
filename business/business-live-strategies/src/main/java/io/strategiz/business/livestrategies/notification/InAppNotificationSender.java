package io.strategiz.business.livestrategies.notification;

import io.strategiz.business.livestrategies.adapter.AlertSignalAdapter.NotificationSender;
import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.data.strategy.entity.AlertDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * In-app notification sender for strategy alerts. Stores notifications in Firestore for
 * display in the app's notification center.
 *
 * In-app notifications are always available and serve as the default channel.
 */
@Component
public class InAppNotificationSender implements NotificationSender {

	private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);

	private static final String CHANNEL = "in-app";

	private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");

	// TODO: Inject InAppNotificationRepository when created
	// private final InAppNotificationRepository notificationRepository;

	public InAppNotificationSender() {
		// TODO: Accept repository in constructor
		log.info("InAppNotificationSender initialized");
	}

	@Override
	public boolean supports(String channel) {
		return CHANNEL.equalsIgnoreCase(channel);
	}

	@Override
	public void send(AlertDeployment alert, Signal signal) {
		String userId = alert.getUserId();
		if (userId == null || userId.isEmpty()) {
			log.warn("No user ID for alert {}, cannot create in-app notification", alert.getId());
			return;
		}

		Map<String, Object> notification = buildNotification(alert, signal);

		// TODO: Save to Firestore when repository is available
		// notificationRepository.save(userId, notification);

		log.info("In-app notification created for user {} alert {}", userId, alert.getId());

		// For now, just log the notification
		log.debug("Notification data: {}", notification);
	}

	/**
	 * Build the in-app notification document.
	 */
	private Map<String, Object> buildNotification(AlertDeployment alert, Signal signal) {
		Map<String, Object> notification = new HashMap<>();

		String strategyName = alert.getAlertName() != null ? alert.getAlertName() : "Strategy Alert";
		String emoji = signal.getType() == Signal.Type.BUY ? "📈" : "📉";
		String price = "$" + PRICE_FORMAT.format(signal.getPrice());

		// Basic fields
		notification.put("type", "STRATEGY_ALERT");
		notification.put("title", String.format("%s %s Signal: %s", emoji, signal.getType(), signal.getSymbol()));
		notification.put("message", String.format("%s @ %s", strategyName, price));
		notification.put("read", false);
		notification.put("createdAt", Instant.now().toString());

		// Alert-specific fields for deep linking
		notification.put("alertId", alert.getId());
		notification.put("strategyId", alert.getStrategyId());
		notification.put("symbol", signal.getSymbol());
		notification.put("signalType", signal.getType().name());

		// Include signal price
		notification.put("price", signal.getPrice());

		// Action
		notification.put("action", "VIEW_ALERT");
		notification.put("actionUrl", "/alerts/" + alert.getId());

		return notification;
	}

}
