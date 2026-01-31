package io.strategiz.service.auth.model.push;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to register a push subscription.
 *
 * @param endpoint the push service endpoint URL
 * @param p256dh the P-256 ECDH key for message encryption
 * @param auth the auth secret for message authentication
 * @param deviceName optional friendly name for the device
 */
public record PushSubscriptionRequest(@NotBlank(message = "Endpoint is required") String endpoint,

		@NotBlank(message = "P256dh key is required") String p256dh,

		@NotBlank(message = "Auth secret is required") String auth,

		String deviceName) {
}
