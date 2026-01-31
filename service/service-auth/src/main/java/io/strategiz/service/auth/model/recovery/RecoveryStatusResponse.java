package io.strategiz.service.auth.model.recovery;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.strategiz.data.auth.entity.RecoveryStatus;

/**
 * Response containing the status of a recovery request.
 *
 * @param found whether the recovery request was found
 * @param status the current status
 * @param mfaRequired whether MFA verification is required
 * @param emailVerified whether email has been verified
 * @param smsVerified whether SMS has been verified
 * @param phoneNumberHint masked phone number hint
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecoveryStatusResponse(boolean found, RecoveryStatus status, Boolean mfaRequired, Boolean emailVerified,
		Boolean smsVerified, String phoneNumberHint) {
	public static RecoveryStatusResponse from(
			io.strategiz.business.tokenauth.AccountRecoveryBusiness.RecoveryStatusResult result) {
		return new RecoveryStatusResponse(result.found(), result.status(), result.mfaRequired(), result.emailVerified(),
				result.smsVerified(), result.phoneNumberHint());
	}
}
