package io.strategiz.service.auth.controller.signup;

import io.strategiz.service.auth.model.signup.EmailSignupInitiateRequest;
import io.strategiz.service.auth.model.signup.EmailSignupVerifyRequest;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
import io.strategiz.service.auth.service.signup.EmailSignupService;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for email-based user signup with OTP verification.
 *
 * Flow:
 * 1. POST /initiate - Start signup, sends OTP to email
 * 2. POST /verify - Verify OTP and complete account creation
 *
 * Admin users can bypass OTP verification when configured.
 */
@RestController
@RequestMapping("/v1/auth/signup/email")
public class EmailSignupController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(EmailSignupController.class);

    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    @Autowired
    private EmailSignupService emailSignupService;

    @Autowired
    private CookieUtil cookieUtil;

    /**
     * Initiate email signup by sending OTP verification code.
     *
     * POST /v1/auth/signup/email/initiate
     *
     * @param request Signup request with name, email, password
     * @return Session ID for verification step
     */
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiateSignup(
            @Valid @RequestBody EmailSignupInitiateRequest request) {

        logRequest("initiateEmailSignup", request.email());

        String sessionId = emailSignupService.initiateSignup(request);

        Map<String, Object> response = Map.of(
            "success", true,
            "sessionId", sessionId,
            "message", "Verification code sent to your email",
            "expiresInMinutes", 10
        );

        logRequestSuccess("initiateEmailSignup", request.email(), response);
        return createCleanResponse(response);
    }

    /**
     * Verify OTP and complete signup.
     *
     * POST /v1/auth/signup/email/verify
     *
     * @param request Verification request with email, OTP code, and session ID
     * @param servletRequest HTTP request for device info
     * @param servletResponse HTTP response for setting cookies
     * @return Signup response with user details and tokens
     */
    @PostMapping("/verify")
    public ResponseEntity<OAuthSignupResponse> verifyAndComplete(
            @Valid @RequestBody EmailSignupVerifyRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        logRequest("verifyEmailSignup", request.email());

        String deviceId = servletRequest.getHeader("X-Device-ID");
        String ipAddress = servletRequest.getRemoteAddr();

        OAuthSignupResponse response = emailSignupService.verifyAndCompleteSignup(
            request.email(),
            request.otpCode(),
            request.sessionId(),
            deviceId,
            ipAddress
        );

        // Set authentication cookies
        if (response.getAccessToken() != null) {
            cookieUtil.setAccessTokenCookie(servletResponse, response.getAccessToken());
        }
        if (response.getRefreshToken() != null) {
            cookieUtil.setRefreshTokenCookie(servletResponse, response.getRefreshToken());
        }

        log.info("Email signup completed for user: {}", response.getUserId());
        logRequestSuccess("verifyEmailSignup", request.email(), response);
        return createCleanResponse(response);
    }
}
