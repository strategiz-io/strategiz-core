package io.strategiz.business.livestrategies.notification;

import io.strategiz.business.livestrategies.adapter.AlertSignalAdapter.NotificationSender;
import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.client.fcm.PushProvider;
import io.strategiz.client.fcm.model.PushDeliveryResult;
import io.strategiz.client.fcm.model.PushMessage;
import io.strategiz.data.strategy.entity.AlertDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;

/**
 * Push notification sender for strategy alerts. Uses Firebase Cloud Messaging to deliver push
 * notifications to mobile and web apps.
 */
@Component
@ConditionalOnBean(PushProvider.class)
public class PushNotificationSender implements NotificationSender {

	private static final Logger log = LoggerFactory.getLogger(PushNotificationSender.class);

	private static final String CHANNEL = "push";

	private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");

	// Android notification channel for alerts
	private static final String ANDROID_CHANNEL_ID = "strategy-alerts";

	private final PushProvider pushProvider;

	public PushNotificationSender(PushProvider pushProvider) {
		this.pushProvider = pushProvider;
		log.info("PushNotificationSender initialized");
	}

	@Override
	public boolean supports(String channel) {
		return CHANNEL.equalsIgnoreCase(channel);
	}

	@Override
	public void send(AlertDeployment alert, Signal signal) {
		if (!pushProvider.isAvailable()) {
			throw new RuntimeException("Push provider not available");
		}

		// Always send to user topic (device tokens would be managed separately)
		String userId = alert.getUserId();
		if (userId != null && !userId.isEmpty()) {
			sendToUserTopic(alert, signal, userId);
			return;
		}

		log.warn("No user ID for alert {}, cannot send push notification", alert.getId());
	}

	/**
	 * Send push notification to user topic (all devices registered to the user).
	 */
	private void sendToUserTopic(AlertDeployment alert, Signal signal, String userId) {
		PushMessage push = buildPushMessage(alert, signal);

		// Topic format: user_{userId}_alerts
		String topic = "user_" + userId + "_alerts";

		PushDeliveryResult result = pushProvider.sendToTopic(push, topic);

		if (!result.isSuccess()) {
			throw new RuntimeException(
					"Push topic delivery failed: " + result.getErrorCode() + " - " + result.getErrorMessage());
		}

		log.info("Push notification sent to topic {} for alert {}", topic, alert.getId());
	}

	/**
	 * Build the push message from alert and signal data.
	 */
	private PushMessage buildPushMessage(AlertDeployment alert, Signal signal) {
		String strategyName = alert.getAlertName() != null ? alert.getAlertName() : "Strategy Alert";

		String emoji = signal.getType() == Signal.Type.BUY ? "📈" : "📉";

		String title = String.format("%s %s Signal: %s", emoji, signal.getType(), signal.getSymbol());

		String price = "$" + PRICE_FORMAT.format(signal.getPrice());

		String body = String.format("%s @ %s", strategyName, price);

		return PushMessage.builder()
			.title(title)
			.body(body)
			.priority(PushMessage.Priority.HIGH)
			.androidChannelId(ANDROID_CHANNEL_ID)
			.iosSound("alert.aiff")
			// Data payload for deep linking
			.addData("type", "strategy_alert")
			.addData("alertId", alert.getId())
			.addData("strategyId", alert.getStrategyId())
			.addData("symbol", signal.getSymbol())
			.addData("signalType", signal.getType().name())
			.addData("clickAction", "OPEN_ALERT")
			.build();
	}

}
