package io.strategiz.service.auth.model.recovery;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response from initiating account recovery.
 *
 * @param success whether the request was accepted
 * @param recoveryId the recovery request ID (for tracking)
 * @param mfaRequired whether SMS verification will be required
 * @param phoneNumberHint masked phone number hint (if MFA required)
 * @param message user-facing message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecoveryInitiateResponse(
        boolean success,
        String recoveryId,
        boolean mfaRequired,
        String phoneNumberHint,
        String message
) {
    public static RecoveryInitiateResponse from(
            io.strategiz.business.tokenauth.AccountRecoveryBusiness.RecoveryInitiationResult result) {
        return new RecoveryInitiateResponse(
                result.success(),
                result.recoveryId(),
                result.mfaRequired(),
                result.phoneNumberHint(),
                result.message()
        );
    }
}
