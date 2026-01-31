package io.strategiz.service.auth.model.push;

/**
 * Response containing the VAPID public key for push subscriptions. The frontend needs
 * this to register with the browser's push service.
 *
 * @param publicKey the VAPID public key (base64url encoded)
 */
public record VapidKeyResponse(String publicKey) {
}
