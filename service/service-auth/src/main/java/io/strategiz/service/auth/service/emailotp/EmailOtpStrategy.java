package io.strategiz.service.auth.service.emailotp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;

/**
 * Production implementation of EmailOTP authentication strategy. This implementation
 * sends actual email OTPs to users. Only active in the production profile.
 */
@Component
@Profile("prod") // Only active in production
public class EmailOtpStrategy implements AuthMethodStrategy {

	private static final Logger logger = LoggerFactory.getLogger(EmailOtpStrategy.class);

	private final EmailOtpAuthenticationService emailOtpAuthenticationService;

	public EmailOtpStrategy(EmailOtpAuthenticationService emailOtpAuthenticationService) {
		this.emailOtpAuthenticationService = emailOtpAuthenticationService;
	}

	@Override
	public Object setupAuthentication(UserEntity user) {
		String email = user.getProfile().getEmail();
		String name = user.getProfile().getName();

		logger.info("Sending signup verification OTP to email: {}", email);

		// Generate and send OTP via email
		boolean otpSent = emailOtpAuthenticationService.sendOtp(email, "SIGNUP_VERIFICATION");

		// Note: In a more advanced implementation, we would want to pass template
		// variables
		// like the user's name for email personalization

		if (!otpSent) {
			logger.error("Failed to send OTP to email: {}", email);
			return Map.of("success", false, "message", "Failed to send verification code");
		}

		logger.info("Successfully sent OTP to email: {}", email);
		return Map.of("success", true, "message", "Verification code sent to your email", "requiresVerification", true,
				"verificationType", "emailotp");
	}

	@Override
	public String getAuthMethodName() {
		return "emailotp";
	}

}
