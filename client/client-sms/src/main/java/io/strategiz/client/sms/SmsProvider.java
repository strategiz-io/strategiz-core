package io.strategiz.client.sms;

import io.strategiz.client.sms.model.SmsDeliveryResult;
import io.strategiz.client.sms.model.SmsMessage;

/**
 * Interface for SMS providers. Allows different implementations (Twilio, AWS SNS, etc.)
 * to be swapped.
 */
public interface SmsProvider {

	/**
	 * Send an SMS message.
	 * @param message The SMS message to send
	 * @return The delivery result with status and message ID
	 */
	SmsDeliveryResult sendSms(SmsMessage message);

	/**
	 * Check if the provider is configured and available.
	 * @return true if the provider can send messages
	 */
	boolean isAvailable();

	/**
	 * Get the provider name for logging and identification.
	 * @return The provider name (e.g., "TWILIO", "AWS_SNS")
	 */
	String getProviderName();

}
