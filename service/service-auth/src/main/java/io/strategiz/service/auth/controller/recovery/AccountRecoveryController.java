package io.strategiz.service.auth.controller.recovery;

import io.strategiz.business.tokenauth.AccountRecoveryBusiness;
import io.strategiz.service.auth.model.recovery.RecoveryCompletionResponse;
import io.strategiz.service.auth.model.recovery.RecoveryInitiateRequest;
import io.strategiz.service.auth.model.recovery.RecoveryInitiateResponse;
import io.strategiz.service.auth.model.recovery.RecoveryStatusResponse;
import io.strategiz.service.auth.model.recovery.RecoveryStepResponse;
import io.strategiz.service.auth.model.recovery.RecoveryVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for account recovery operations.
 *
 * <p>Account recovery flow:</p>
 * <ol>
 *   <li>POST /v1/auth/recovery/initiate - Start recovery with email</li>
 *   <li>POST /v1/auth/recovery/{recoveryId}/verify-email - Verify email code</li>
 *   <li>POST /v1/auth/recovery/{recoveryId}/verify-sms - Verify SMS code (if MFA)</li>
 *   <li>Use recovery token to reset account</li>
 * </ol>
 *
 * <p>Recovery tokens have:</p>
 * <ul>
 *   <li>token_type: "recovery"</li>
 *   <li>scope: "account:recover"</li>
 *   <li>acr: "0" (unauthenticated)</li>
 *   <li>15-minute validity</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/auth/recovery")
@Tag(name = "Account Recovery", description = "Account recovery operations")
public class AccountRecoveryController {

    private static final Logger log = LoggerFactory.getLogger(AccountRecoveryController.class);

    @Autowired
    private AccountRecoveryBusiness accountRecoveryBusiness;

    /**
     * Initiate account recovery.
     *
     * <p>Sends a verification code to the email address if it's registered.
     * Returns the same response regardless of whether the email exists
     * to prevent email enumeration attacks.</p>
     *
     * @param request the recovery request containing email
     * @param httpRequest the HTTP request for IP/user-agent
     * @return recovery initiation response
     */
    @PostMapping("/initiate")
    @Operation(summary = "Initiate account recovery",
            description = "Start the account recovery process. An email verification code will be sent.")
    @ApiResponse(responseCode = "200", description = "Recovery initiated (or email not found - same response)")
    public ResponseEntity<RecoveryInitiateResponse> initiateRecovery(
            @Valid @RequestBody RecoveryInitiateRequest request,
            HttpServletRequest httpRequest) {

        log.info("Recovery initiation request received");

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AccountRecoveryBusiness.RecoveryInitiationResult result =
                accountRecoveryBusiness.initiateRecovery(request.email(), ipAddress, userAgent);

        return ResponseEntity.ok(RecoveryInitiateResponse.from(result));
    }

    /**
     * Verify email code for recovery.
     *
     * <p>If email verification succeeds and MFA is enabled, an SMS code
     * will be sent to the recovery phone. If MFA is not enabled, a
     * recovery token will be issued immediately.</p>
     *
     * @param recoveryId the recovery request ID
     * @param request the verification request containing the code
     * @return step response indicating next step or completion
     */
    @PostMapping("/{recoveryId}/verify-email")
    @Operation(summary = "Verify email code",
            description = "Verify the email verification code. Returns next step (SMS or complete).")
    @ApiResponse(responseCode = "200", description = "Verification result")
    public ResponseEntity<RecoveryStepResponse> verifyEmailCode(
            @Parameter(description = "Recovery request ID") @PathVariable String recoveryId,
            @Valid @RequestBody RecoveryVerifyRequest request) {

        log.info("Email verification request for recovery: {}", recoveryId);

        AccountRecoveryBusiness.RecoveryStepResult result =
                accountRecoveryBusiness.verifyEmailCode(recoveryId, request.code());

        return ResponseEntity.ok(RecoveryStepResponse.from(result));
    }

    /**
     * Verify SMS code for recovery.
     *
     * <p>This step is only required if MFA is enabled on the account.
     * On success, a recovery token is issued.</p>
     *
     * @param recoveryId the recovery request ID
     * @param request the verification request containing the code
     * @return completion response with recovery token
     */
    @PostMapping("/{recoveryId}/verify-sms")
    @Operation(summary = "Verify SMS code",
            description = "Verify the SMS verification code (required if MFA enabled).")
    @ApiResponse(responseCode = "200", description = "Verification result with recovery token if successful")
    public ResponseEntity<RecoveryCompletionResponse> verifySmsCode(
            @Parameter(description = "Recovery request ID") @PathVariable String recoveryId,
            @Valid @RequestBody RecoveryVerifyRequest request) {

        log.info("SMS verification request for recovery: {}", recoveryId);

        AccountRecoveryBusiness.RecoveryCompletionResult result =
                accountRecoveryBusiness.verifySmsCode(recoveryId, request.code());

        return ResponseEntity.ok(RecoveryCompletionResponse.from(result));
    }

    /**
     * Get recovery request status.
     *
     * @param recoveryId the recovery request ID
     * @return the current status of the recovery request
     */
    @GetMapping("/{recoveryId}/status")
    @Operation(summary = "Get recovery status",
            description = "Get the current status of a recovery request.")
    @ApiResponse(responseCode = "200", description = "Recovery status")
    public ResponseEntity<RecoveryStatusResponse> getRecoveryStatus(
            @Parameter(description = "Recovery request ID") @PathVariable String recoveryId) {

        log.info("Status request for recovery: {}", recoveryId);

        AccountRecoveryBusiness.RecoveryStatusResult result =
                accountRecoveryBusiness.getRecoveryStatus(recoveryId);

        return ResponseEntity.ok(RecoveryStatusResponse.from(result));
    }

    /**
     * Cancel a recovery request.
     *
     * @param recoveryId the recovery request ID
     * @return success status
     */
    @DeleteMapping("/{recoveryId}")
    @Operation(summary = "Cancel recovery",
            description = "Cancel an active recovery request.")
    @ApiResponse(responseCode = "200", description = "Cancellation result")
    public ResponseEntity<Void> cancelRecovery(
            @Parameter(description = "Recovery request ID") @PathVariable String recoveryId) {

        log.info("Cancel request for recovery: {}", recoveryId);

        boolean cancelled = accountRecoveryBusiness.cancelRecovery(recoveryId);

        if (cancelled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Resend email verification code.
     *
     * @param recoveryId the recovery request ID
     * @return success status
     */
    @PostMapping("/{recoveryId}/resend-email")
    @Operation(summary = "Resend email code",
            description = "Resend the email verification code.")
    @ApiResponse(responseCode = "200", description = "Resend result")
    public ResponseEntity<Void> resendEmailCode(
            @Parameter(description = "Recovery request ID") @PathVariable String recoveryId) {

        log.info("Resend email code request for recovery: {}", recoveryId);

        var result = accountRecoveryBusiness.resendEmailCode(recoveryId);

        if (result.isPresent()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Resend SMS verification code.
     *
     * @param recoveryId the recovery request ID
     * @return success status
     */
    @PostMapping("/{recoveryId}/resend-sms")
    @Operation(summary = "Resend SMS code",
            description = "Resend the SMS verification code.")
    @ApiResponse(responseCode = "200", description = "Resend result")
    public ResponseEntity<Void> resendSmsCode(
            @Parameter(description = "Recovery request ID") @PathVariable String recoveryId) {

        log.info("Resend SMS code request for recovery: {}", recoveryId);

        var result = accountRecoveryBusiness.resendSmsCode(recoveryId);

        if (result.isPresent()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Extract client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
