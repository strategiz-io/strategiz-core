package io.strategiz.client.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import io.strategiz.client.fcm.model.PushDeliveryResult;
import io.strategiz.client.fcm.model.PushMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Firebase Cloud Messaging implementation of PushProvider.
 *
 * Uses Firebase Admin SDK which automatically uses application default credentials on
 * Cloud Run.
 */
@Component
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true", matchIfMissing = true)
public class FcmClient implements PushProvider {

	private static final Logger logger = LoggerFactory.getLogger(FcmClient.class);

	private static final String PROVIDER_NAME = "FCM";

	private final FcmConfig config;

	private FirebaseMessaging messaging;

	private boolean initialized = false;

	public FcmClient(FcmConfig config) {
		this.config = config;
	}

	@PostConstruct
	public void init() {
		if (config.isConfigured()) {
			try {
				// Check if FirebaseApp is already initialized (by client-firebase module)
				FirebaseApp app;
				if (FirebaseApp.getApps().isEmpty()) {
					logger.warn("FirebaseApp not initialized - FCM requires Firebase to be configured");
					return;
				}
				app = FirebaseApp.getInstance();

				messaging = FirebaseMessaging.getInstance(app);
				initialized = true;
				logger.info("FCM client initialized successfully");
			}
			catch (Exception e) {
				logger.error("Failed to initialize FCM client: {}", e.getMessage());
				initialized = false;
			}
		}
		else {
			logger.warn("FCM client not configured - push notifications will be disabled");
		}
	}

	@Override
	public PushDeliveryResult sendPush(PushMessage pushMessage) {
		if (!isAvailable()) {
			logger.warn("FCM client not available, cannot send push notification");
			return PushDeliveryResult.unavailable(PROVIDER_NAME);
		}

		if (pushMessage.getDeviceToken() == null || pushMessage.getDeviceToken().isEmpty()) {
			return PushDeliveryResult.failure("INVALID_TOKEN", "Device token is required", PROVIDER_NAME);
		}

		// Mock mode for development
		if (config.isMockEnabled()) {
			logger.info("[MOCK PUSH] To: {}, Title: {}", maskToken(pushMessage.getDeviceToken()),
					pushMessage.getTitle());
			return PushDeliveryResult.success("mock-" + System.currentTimeMillis(), PROVIDER_NAME);
		}

		try {
			Message message = buildMessage(pushMessage);

			logger.info("Sending push via FCM to {}", maskToken(pushMessage.getDeviceToken()));

			String messageId = messaging.send(message);

			logger.info("Push sent successfully. MessageId: {}", messageId);

			return PushDeliveryResult.success(messageId, PROVIDER_NAME);
		}
		catch (FirebaseMessagingException e) {
			logger.error("FCM API error: {} (code: {})", e.getMessage(), e.getMessagingErrorCode());
			return PushDeliveryResult.failure(e.getMessagingErrorCode().name(), e.getMessage(), PROVIDER_NAME);
		}
		catch (Exception e) {
			logger.error("Unexpected error sending push via FCM", e);
			return PushDeliveryResult.failure("UNKNOWN", e.getMessage(), PROVIDER_NAME);
		}
	}

	@Override
	public PushDeliveryResult sendMulticast(PushMessage pushMessage, List<String> deviceTokens) {
		if (!isAvailable()) {
			logger.warn("FCM client not available, cannot send multicast");
			return PushDeliveryResult.unavailable(PROVIDER_NAME);
		}

		if (deviceTokens == null || deviceTokens.isEmpty()) {
			return PushDeliveryResult.failure("NO_TOKENS", "At least one device token is required", PROVIDER_NAME);
		}

		// Mock mode for development
		if (config.isMockEnabled()) {
			logger.info("[MOCK MULTICAST] To {} devices, Title: {}", deviceTokens.size(), pushMessage.getTitle());
			return PushDeliveryResult.multicastSuccess(deviceTokens.size(), 0, List.of(), PROVIDER_NAME);
		}

		try {
			MulticastMessage message = buildMulticastMessage(pushMessage, deviceTokens);

			logger.info("Sending multicast push via FCM to {} devices", deviceTokens.size());

			BatchResponse response = messaging.sendEachForMulticast(message);

			List<String> failedTokens = new ArrayList<>();
			int index = 0;
			for (SendResponse sendResponse : response.getResponses()) {
				if (!sendResponse.isSuccessful()) {
					failedTokens.add(deviceTokens.get(index));
				}
				index++;
			}

			logger.info("Multicast complete. Success: {}, Failure: {}", response.getSuccessCount(),
					response.getFailureCount());

			return PushDeliveryResult.multicastSuccess(response.getSuccessCount(), response.getFailureCount(),
					failedTokens, PROVIDER_NAME);
		}
		catch (FirebaseMessagingException e) {
			logger.error("FCM multicast error: {} (code: {})", e.getMessage(), e.getMessagingErrorCode());
			return PushDeliveryResult.failure(e.getMessagingErrorCode().name(), e.getMessage(), PROVIDER_NAME);
		}
		catch (Exception e) {
			logger.error("Unexpected error sending multicast via FCM", e);
			return PushDeliveryResult.failure("UNKNOWN", e.getMessage(), PROVIDER_NAME);
		}
	}

	@Override
	public PushDeliveryResult sendToTopic(PushMessage pushMessage, String topic) {
		if (!isAvailable()) {
			logger.warn("FCM client not available, cannot send to topic");
			return PushDeliveryResult.unavailable(PROVIDER_NAME);
		}

		if (topic == null || topic.isEmpty()) {
			return PushDeliveryResult.failure("INVALID_TOPIC", "Topic is required", PROVIDER_NAME);
		}

		// Mock mode for development
		if (config.isMockEnabled()) {
			logger.info("[MOCK TOPIC] Topic: {}, Title: {}", topic, pushMessage.getTitle());
			return PushDeliveryResult.success("mock-topic-" + System.currentTimeMillis(), PROVIDER_NAME);
		}

		try {
			Message message = buildTopicMessage(pushMessage, topic);

			logger.info("Sending topic push via FCM to topic '{}'", topic);

			String messageId = messaging.send(message);

			logger.info("Topic push sent successfully. MessageId: {}", messageId);

			return PushDeliveryResult.success(messageId, PROVIDER_NAME);
		}
		catch (FirebaseMessagingException e) {
			logger.error("FCM topic error: {} (code: {})", e.getMessage(), e.getMessagingErrorCode());
			return PushDeliveryResult.failure(e.getMessagingErrorCode().name(), e.getMessage(), PROVIDER_NAME);
		}
		catch (Exception e) {
			logger.error("Unexpected error sending topic push via FCM", e);
			return PushDeliveryResult.failure("UNKNOWN", e.getMessage(), PROVIDER_NAME);
		}
	}

	@Override
	public boolean isAvailable() {
		return config.isEnabled() && (initialized || config.isMockEnabled());
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	/**
	 * Build FCM Message for single device.
	 */
	private Message buildMessage(PushMessage pushMessage) {
		Message.Builder builder = Message.builder().setToken(pushMessage.getDeviceToken());

		addNotificationAndConfig(builder, pushMessage);

		return builder.build();
	}

	/**
	 * Build FCM MulticastMessage for multiple devices.
	 */
	private MulticastMessage buildMulticastMessage(PushMessage pushMessage, List<String> tokens) {
		MulticastMessage.Builder builder = MulticastMessage.builder().addAllTokens(tokens);

		// Notification
		Notification.Builder notifBuilder = Notification.builder()
			.setTitle(pushMessage.getTitle())
			.setBody(pushMessage.getBody());

		if (pushMessage.getImageUrl() != null) {
			notifBuilder.setImage(pushMessage.getImageUrl());
		}

		builder.setNotification(notifBuilder.build());

		// Data payload
		if (pushMessage.getData() != null) {
			builder.putAllData(pushMessage.getData());
		}

		// Android config
		builder.setAndroidConfig(buildAndroidConfig(pushMessage));

		// iOS config
		builder.setApnsConfig(buildApnsConfig(pushMessage));

		return builder.build();
	}

	/**
	 * Build FCM Message for topic.
	 */
	private Message buildTopicMessage(PushMessage pushMessage, String topic) {
		Message.Builder builder = Message.builder().setTopic(topic);

		addNotificationAndConfig(builder, pushMessage);

		return builder.build();
	}

	/**
	 * Add notification and platform configs to message builder.
	 */
	private void addNotificationAndConfig(Message.Builder builder, PushMessage pushMessage) {
		// Notification
		Notification.Builder notifBuilder = Notification.builder()
			.setTitle(pushMessage.getTitle())
			.setBody(pushMessage.getBody());

		if (pushMessage.getImageUrl() != null) {
			notifBuilder.setImage(pushMessage.getImageUrl());
		}

		builder.setNotification(notifBuilder.build());

		// Data payload
		if (pushMessage.getData() != null) {
			builder.putAllData(pushMessage.getData());
		}

		// Android config
		builder.setAndroidConfig(buildAndroidConfig(pushMessage));

		// iOS config
		builder.setApnsConfig(buildApnsConfig(pushMessage));
	}

	/**
	 * Build Android-specific configuration.
	 */
	private AndroidConfig buildAndroidConfig(PushMessage pushMessage) {
		String channelId = pushMessage.getAndroidChannelId() != null ? pushMessage.getAndroidChannelId()
				: config.getDefaultAndroidChannel();

		AndroidNotification.Builder androidNotif = AndroidNotification.builder().setChannelId(channelId);

		if (pushMessage.getClickAction() != null) {
			androidNotif.setClickAction(pushMessage.getClickAction());
		}

		AndroidConfig.Priority priority = pushMessage.getPriority() == PushMessage.Priority.HIGH
				? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL;

		return AndroidConfig.builder()
			.setTtl(config.getTimeToLive() * 1000L) // Convert to milliseconds
			.setPriority(priority)
			.setNotification(androidNotif.build())
			.build();
	}

	/**
	 * Build iOS-specific configuration (APNS).
	 */
	private ApnsConfig buildApnsConfig(PushMessage pushMessage) {
		String sound = pushMessage.getIosSound() != null ? pushMessage.getIosSound() : config.getDefaultIosSound();

		Aps.Builder apsBuilder = Aps.builder().setSound(sound);

		if (pushMessage.getIosBadge() != null) {
			try {
				apsBuilder.setBadge(Integer.parseInt(pushMessage.getIosBadge()));
			}
			catch (NumberFormatException ignored) {
			}
		}

		return ApnsConfig.builder().setAps(apsBuilder.build()).build();
	}

	/**
	 * Mask device token for secure logging.
	 */
	private String maskToken(String token) {
		if (token == null || token.length() < 8) {
			return "****";
		}
		return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
	}

}
