package io.strategiz.service.auth.model.recovery;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.strategiz.business.tokenauth.AccountRecoveryBusiness;

/**
 * Response from completing recovery authentication.
 * Returns user info after successful authentication via recovery.
 *
 * @param success whether authentication was successful
 * @param userId the authenticated user ID
 * @param email the user's email
 * @param name the user's display name
 * @param message user-facing message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecoveryAuthResponse(
        boolean success,
        String userId,
        String email,
        String name,
        String message
) {
    public static RecoveryAuthResponse from(AccountRecoveryBusiness.RecoveryAuthResult result) {
        return new RecoveryAuthResponse(
                result.success(),
                result.userId(),
                result.email(),
                result.name(),
                result.message()
        );
    }
}
