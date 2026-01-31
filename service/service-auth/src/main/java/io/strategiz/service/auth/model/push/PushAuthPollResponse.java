package io.strategiz.service.auth.model.push;

import io.strategiz.business.tokenauth.PushAuthBusiness;
import io.strategiz.data.auth.entity.PushAuthStatus;

import java.time.Instant;

/**
 * Response for polling push auth status.
 *
 * @param status the current status (PENDING, APPROVED, DENIED, EXPIRED, CANCELLED)
 * @param expiresAt when the request expires (for PENDING)
 * @param respondedAt when the user responded (for APPROVED/DENIED)
 * @param error error message if any
 */
public record PushAuthPollResponse(PushAuthStatus status, Instant expiresAt, Instant respondedAt, String error) {
	public static PushAuthPollResponse from(PushAuthBusiness.PollResult result) {
		if (result.error() != null) {
			return new PushAuthPollResponse(result.status(), null, null, result.error());
		}

		return new PushAuthPollResponse(result.status(),
				result.request() != null ? result.request().getExpiresAt() : null,
				result.request() != null ? result.request().getRespondedAt() : null, null);
	}
}
