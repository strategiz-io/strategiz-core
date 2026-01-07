package io.strategiz.client.sendgrid;

import io.strategiz.client.sendgrid.model.EmailDeliveryResult;
import io.strategiz.client.sendgrid.model.EmailMessage;

/**
 * Interface for email providers.
 * Allows different implementations (SendGrid, AWS SES, etc.) to be swapped.
 */
public interface EmailProvider {

	/**
	 * Send an email message.
	 * @param message The email message to send
	 * @return The delivery result with status and message ID
	 */
	EmailDeliveryResult sendEmail(EmailMessage message);

	/**
	 * Check if the provider is configured and available.
	 * @return true if the provider can send messages
	 */
	boolean isAvailable();

	/**
	 * Get the provider name for logging and identification.
	 * @return The provider name (e.g., "SENDGRID", "AWS_SES")
	 */
	String getProviderName();

}
