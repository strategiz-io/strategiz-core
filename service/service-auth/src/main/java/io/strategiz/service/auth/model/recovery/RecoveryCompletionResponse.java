package io.strategiz.service.auth.model.recovery;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response from completing account recovery (SMS verification).
 *
 * @param success whether recovery was completed
 * @param recoveryToken the recovery token for account actions
 * @param message user-facing message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecoveryCompletionResponse(boolean success, String recoveryToken, String message) {
	public static RecoveryCompletionResponse from(
			io.strategiz.business.tokenauth.AccountRecoveryBusiness.RecoveryCompletionResult result) {
		return new RecoveryCompletionResponse(result.success(), result.recoveryToken(), result.message());
	}
}
