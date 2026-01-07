package io.strategiz.client.fcm;

import io.strategiz.client.fcm.model.PushDeliveryResult;
import io.strategiz.client.fcm.model.PushMessage;

import java.util.List;

/**
 * Interface for push notification providers.
 * Allows different implementations (FCM, APNS, etc.) to be swapped.
 */
public interface PushProvider {

	/**
	 * Send a push notification to a single device.
	 * @param message The push message to send
	 * @return The delivery result with status and message ID
	 */
	PushDeliveryResult sendPush(PushMessage message);

	/**
	 * Send push notification to multiple devices (multicast).
	 * @param message The push message to send
	 * @param deviceTokens List of device tokens to send to
	 * @return The delivery result with batch results
	 */
	PushDeliveryResult sendMulticast(PushMessage message, List<String> deviceTokens);

	/**
	 * Send push notification to a topic.
	 * @param message The push message to send
	 * @param topic The topic name (e.g., "alerts", "signals")
	 * @return The delivery result
	 */
	PushDeliveryResult sendToTopic(PushMessage message, String topic);

	/**
	 * Check if the provider is configured and available.
	 * @return true if the provider can send messages
	 */
	boolean isAvailable();

	/**
	 * Get the provider name for logging and identification.
	 * @return The provider name (e.g., "FCM", "APNS")
	 */
	String getProviderName();

}
