package io.strategiz.service.auth.model.push;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to approve or deny a push auth request.
 * Called from the device receiving the push notification.
 *
 * @param challenge the challenge token from the push notification
 * @param approved true to approve, false to deny
 * @param subscriptionId the subscription ID that responded (optional)
 */
public record PushAuthRespondRequest(
        @NotBlank(message = "Challenge is required")
        String challenge,

        boolean approved,

        String subscriptionId
) {
}
