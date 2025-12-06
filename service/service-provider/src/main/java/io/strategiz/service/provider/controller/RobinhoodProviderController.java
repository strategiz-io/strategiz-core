package io.strategiz.service.provider.controller;

import io.strategiz.business.provider.robinhood.RobinhoodProviderBusiness;
import io.strategiz.business.provider.robinhood.model.RobinhoodConnectionResult;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Robinhood provider integration.
 *
 * Unlike OAuth providers (Coinbase, Schwab), Robinhood uses a credential-based
 * authentication flow that requires:
 * 1. User submits username/password
 * 2. If MFA is enabled, user receives code via SMS/email
 * 3. User submits MFA code to complete authentication
 *
 * Note: This is an unofficial API integration.
 */
@RestController
@RequestMapping("/v1/providers/robinhood")
public class RobinhoodProviderController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.PROVIDER_MODULE;
    }

    private final RobinhoodProviderBusiness robinhoodProviderBusiness;
    private final SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    public RobinhoodProviderController(RobinhoodProviderBusiness robinhoodProviderBusiness,
                                       SessionAuthBusiness sessionAuthBusiness) {
        this.robinhoodProviderBusiness = robinhoodProviderBusiness;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    /**
     * Initiate Robinhood login with credentials.
     *
     * Request body:
     * {
     *   "username": "user@email.com",
     *   "password": "password",
     *   "mfaType": "sms" // or "email"
     * }
     *
     * Response (if MFA required):
     * {
     *   "status": "MFA_REQUIRED",
     *   "challengeId": "xxx",
     *   "challengeType": "sms",
     *   "deviceToken": "xxx",
     *   "message": "MFA code sent to your phone"
     * }
     *
     * Response (if successful):
     * {
     *   "status": "SUCCESS",
     *   "message": "Connected successfully"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> initiateLogin(
            @RequestBody Map<String, String> request,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserId(principal, authHeader);
        log.info("Robinhood login initiated for user: {}", userId);

        String username = request.get("username");
        String password = request.get("password");
        String mfaType = request.getOrDefault("mfaType", "sms");

        if (username == null || username.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD,
                "service-provider", "username");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD,
                "service-provider", "password");
        }

        RobinhoodConnectionResult result = robinhoodProviderBusiness.initiateLogin(
            userId, username, password, mfaType
        );

        Map<String, Object> responseData = buildLoginResponse(result);

        return ResponseEntity.ok(responseData);
    }

    /**
     * Complete MFA challenge with the code received via SMS/email.
     *
     * Request body:
     * {
     *   "challengeId": "xxx",
     *   "mfaCode": "123456",
     *   "deviceToken": "xxx"
     * }
     */
    @PostMapping("/mfa")
    public ResponseEntity<Map<String, Object>> completeMfa(
            @RequestBody Map<String, String> request,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserId(principal, authHeader);
        log.info("Robinhood MFA completion for user: {}", userId);

        String challengeId = request.get("challengeId");
        String mfaCode = request.get("mfaCode");
        String deviceToken = request.get("deviceToken");

        if (challengeId == null || challengeId.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD,
                "service-provider", "challengeId");
        }
        if (mfaCode == null || mfaCode.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD,
                "service-provider", "mfaCode");
        }
        if (deviceToken == null || deviceToken.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD,
                "service-provider", "deviceToken");
        }

        RobinhoodConnectionResult result = robinhoodProviderBusiness.completeMfaChallenge(
            userId, challengeId, mfaCode, deviceToken
        );

        Map<String, Object> responseData = buildLoginResponse(result);

        return ResponseEntity.ok(responseData);
    }

    /**
     * Sync Robinhood portfolio data.
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncData(
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserId(principal, authHeader);
        log.info("Robinhood sync requested for user: {}", userId);

        robinhoodProviderBusiness.syncProviderData(userId);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("status", "SUCCESS");
        responseData.put("message", "Portfolio data synced successfully");

        return ResponseEntity.ok(responseData);
    }

    /**
     * Extract user ID from principal or auth header.
     */
    private String extractUserId(Principal principal, String authHeader) {
        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    log.info("Robinhood request authenticated via Bearer token for user: {}", userId);
                }
            } catch (Exception e) {
                log.warn("Error validating Bearer token for Robinhood: {}", e.getMessage());
            }
        }

        if (userId == null) {
            log.error("No valid authentication session or token for Robinhood operation");
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_INVALID_CREDENTIALS,
                "service-provider", "Authentication required");
        }

        return userId;
    }

    /**
     * Build response from connection result.
     */
    private Map<String, Object> buildLoginResponse(RobinhoodConnectionResult result) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("status", result.getConnectionStatus().name());

        switch (result.getConnectionStatus()) {
            case SUCCESS:
                responseData.put("message", "Successfully connected to Robinhood");
                responseData.put("providerId", result.getProviderId());
                responseData.put("providerName", result.getProviderName());
                if (result.getAccountInfo() != null) {
                    responseData.put("accountInfo", result.getAccountInfo());
                }
                break;

            case MFA_REQUIRED:
                responseData.put("message", "MFA verification required. Check your " +
                    (result.getChallengeType() != null ? result.getChallengeType() : "phone/email") +
                    " for the verification code.");
                responseData.put("deviceToken", result.getDeviceToken());
                if (result.getChallenge() != null) {
                    responseData.put("challengeId", result.getChallenge().getId());
                    responseData.put("challengeType", result.getChallenge().getType());
                } else {
                    responseData.put("challengeType", result.getChallengeType());
                }
                break;

            case DEVICE_APPROVAL:
                responseData.put("message", "Please approve this device in your Robinhood mobile app");
                responseData.put("deviceToken", result.getDeviceToken());
                break;

            case ERROR:
                responseData.put("message", result.getErrorMessage());
                responseData.put("errorCode", result.getErrorCode());
                break;

            default:
                responseData.put("message", "Unknown status");
        }

        return responseData;
    }
}
