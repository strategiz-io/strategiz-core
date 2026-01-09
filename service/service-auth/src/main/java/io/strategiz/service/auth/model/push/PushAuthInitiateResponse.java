package io.strategiz.service.auth.model.push;

import io.strategiz.business.tokenauth.PushAuthBusiness;

import java.time.Instant;

/**
 * Response for push auth initiation.
 *
 * @param success whether initiation was successful
 * @param requestId the push auth request ID for polling
 * @param expiresAt when the request expires
 * @param devicesNotified number of devices notified
 * @param error error message if not successful
 */
public record PushAuthInitiateResponse(
        boolean success,
        String requestId,
        Instant expiresAt,
        int devicesNotified,
        String error
) {
    public static PushAuthInitiateResponse from(PushAuthBusiness.InitiatePushAuthResult result) {
        if (!result.success()) {
            return new PushAuthInitiateResponse(
                    false,
                    null,
                    null,
                    0,
                    result.error()
            );
        }

        return new PushAuthInitiateResponse(
                true,
                result.request().getId(),
                result.request().getExpiresAt(),
                result.subscriptionsToNotify().size(),
                null
        );
    }
}
