package io.strategiz.service.auth.controller.emailotp;

import io.strategiz.service.auth.service.emailotp.EmailOtpAuthenticationService;
import io.strategiz.service.auth.service.emailotp.EmailOtpRegistrationService;
import io.strategiz.service.auth.model.emailotp.EmailOtpRequest;
import io.strategiz.service.auth.model.emailotp.EmailOtpVerificationRequest;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import io.strategiz.framework.exception.StrategizException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controller for Email OTP authentication operations. Handles sending and verifying
 * email-based one-time passwords. Uses clean architecture - returns resources directly,
 * no wrappers.
 */
@RestController
@RequestMapping("/v1/auth/emailotp")
public class EmailOtpController {

	private static final Logger log = LoggerFactory.getLogger(EmailOtpController.class);

	private final EmailOtpAuthenticationService emailOtpAuthService;

	private final EmailOtpRegistrationService emailOtpRegistrationService;

	public EmailOtpController(EmailOtpAuthenticationService emailOtpAuthService,
			EmailOtpRegistrationService emailOtpRegistrationService) {
		this.emailOtpAuthService = emailOtpAuthService;
		this.emailOtpRegistrationService = emailOtpRegistrationService;
	}

	/**
	 * Send OTP to email
	 * @param request Email OTP request containing email address
	 * @return Clean success response - no wrapper, let GlobalExceptionHandler handle
	 * errors
	 */
	@PostMapping("/send")
	public ResponseEntity<Void> sendOtp(@Valid @RequestBody EmailOtpRequest request) {
		log.info("Sending OTP to email: {}", request.email());

		// Send OTP - let exceptions bubble up
		boolean sent = emailOtpAuthService.sendOtp(request.email(), request.purpose());

		if (!sent) {
			throw new StrategizException(ServiceAuthErrorDetails.EMAIL_SEND_FAILED, "service-auth", request.email(),
					"Unknown error");
		}

		// Return clean response - headers added by StandardHeadersInterceptor
		return ResponseEntity.ok().build();
	}

	/**
	 * Verify email OTP
	 * @param request Email OTP verification request containing email and OTP code
	 * @return Clean verification response with result data - no wrapper, let
	 * GlobalExceptionHandler handle errors
	 */
	@PostMapping("/verify")
	public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody EmailOtpVerificationRequest request) {
		log.info("Verifying OTP for email: {}", request.email());

		// Verify OTP - let exceptions bubble up
		boolean verified = emailOtpAuthService.verifyOtp(request.email(), request.purpose(), request.code());

		Map<String, Object> result = Map.of("verified", verified, "email", request.email(), "purpose",
				request.purpose());

		// Return clean response - headers added by StandardHeadersInterceptor
		return ResponseEntity.ok(result);
	}

	/**
	 * Check if there's a pending OTP for an email
	 * @param email The email address to check
	 * @param purpose The purpose to check for
	 * @return Clean status response - no wrapper, let GlobalExceptionHandler handle
	 * errors
	 */
	@GetMapping("/status")
	public ResponseEntity<Boolean> checkOtpStatus(@RequestParam String email, @RequestParam String purpose) {
		log.info("Checking OTP status for email: {} and purpose: {}", email, purpose);

		// Check status - let exceptions bubble up
		boolean hasPending = emailOtpAuthService.hasPendingOtp(email, purpose);

		// Return clean response - headers added by StandardHeadersInterceptor
		return ResponseEntity.ok(hasPending);
	}

}
