package io.strategiz.service.auth.model.recovery;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response from a recovery verification step.
 *
 * @param success whether the verification succeeded
 * @param nextStep the next step ("SMS" or "COMPLETE")
 * @param phoneNumberHint masked phone number (if SMS verification needed)
 * @param recoveryToken the recovery token (if complete)
 * @param message user-facing message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecoveryStepResponse(
        boolean success,
        String nextStep,
        String phoneNumberHint,
        String recoveryToken,
        String message
) {
    public static RecoveryStepResponse from(
            io.strategiz.business.tokenauth.AccountRecoveryBusiness.RecoveryStepResult result) {
        return new RecoveryStepResponse(
                result.success(),
                result.nextStep(),
                result.phoneNumberHint(),
                result.recoveryToken(),
                result.message()
        );
    }
}
