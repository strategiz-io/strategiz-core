package io.strategiz.service.auth.service.fraud;

import io.strategiz.client.recaptcha.RecaptchaClient;
import io.strategiz.client.recaptcha.model.RiskAnalysis;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Service for fraud detection during signup and authentication flows.
 *
 * Uses Google reCAPTCHA Enterprise to assess risk and block suspicious activity.
 */
@Service
public class FraudDetectionService {

	private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);

	private RecaptchaClient recaptchaClient;

	@Value("${recaptcha.threshold:0.5}")
	private double threshold;

	@Value("${recaptcha.enabled:false}")
	private boolean enabled;

	@Value("${recaptcha.block-on-failure:false}")
	private boolean blockOnFailure;

	public FraudDetectionService() {
		// Default constructor - recaptchaClient will be null if not configured
	}

	@Autowired(required = false)
	public void setRecaptchaClient(@Nullable RecaptchaClient recaptchaClient) {
		this.recaptchaClient = recaptchaClient;
		if (recaptchaClient != null) {
			logger.info("FraudDetectionService initialized with RecaptchaClient");
		}
		else {
			logger.info("FraudDetectionService initialized without RecaptchaClient - fraud detection disabled");
		}
	}

	/**
	 * Verify a reCAPTCHA token for a signup action.
	 * @param token the reCAPTCHA token from the frontend
	 * @param email the email being signed up (for logging)
	 * @throws StrategizException if fraud is detected
	 */
	public void verifySignup(String token, String email) {
		verifyAction(token, "signup", email);
	}

	/**
	 * Verify a reCAPTCHA token for a login action.
	 * @param token the reCAPTCHA token from the frontend
	 * @param email the email attempting to login (for logging)
	 * @throws StrategizException if fraud is detected
	 */
	public void verifyLogin(String token, String email) {
		verifyAction(token, "login", email);
	}

	/**
	 * Verify a reCAPTCHA token for any action.
	 * @param token the reCAPTCHA token from the frontend
	 * @param action the action name (e.g., "signup", "login")
	 * @param identifier user identifier for logging (e.g., email)
	 * @throws StrategizException if fraud is detected
	 */
	public void verifyAction(String token, String action, String identifier) {
		// Skip if reCAPTCHA is disabled
		if (!enabled) {
			logger.debug("reCAPTCHA verification disabled - skipping check for {}", action);
			return;
		}

		// Skip if no client available
		if (recaptchaClient == null || !recaptchaClient.isAvailable()) {
			if (blockOnFailure) {
				logger.error("reCAPTCHA client not available and block-on-failure is enabled");
				throw new StrategizException(AuthErrors.RECAPTCHA_FAILED, "reCAPTCHA service unavailable");
			}
			logger.warn("reCAPTCHA client not available - allowing request for {} (identifier: {})", action,
					maskIdentifier(identifier));
			return;
		}

		// Skip if no token provided
		if (token == null || token.isEmpty()) {
			if (blockOnFailure) {
				logger.warn("No reCAPTCHA token provided for {} - blocking (identifier: {})", action,
						maskIdentifier(identifier));
				throw new StrategizException(AuthErrors.RECAPTCHA_FAILED, "reCAPTCHA token required");
			}
			logger.warn("No reCAPTCHA token provided for {} - allowing (identifier: {})", action,
					maskIdentifier(identifier));
			return;
		}

		// Perform the assessment
		RiskAnalysis analysis = recaptchaClient.createAssessment(token, action);

		// Log the result
		logger.info("reCAPTCHA assessment for {}: score={}, valid={}, identifier={}", action, analysis.getScore(),
				analysis.isValid(), maskIdentifier(identifier));

		// Check if the assessment indicates fraud
		if (analysis.isLikelyBot(threshold)) {
			logger.warn("Fraud detected for {} - score {} below threshold {} (identifier: {})", action,
					analysis.getScore(), threshold, maskIdentifier(identifier));

			throw new StrategizException(AuthErrors.FRAUD_DETECTED,
					"Suspicious activity detected. Please try again later.");
		}

		logger.debug("reCAPTCHA verification passed for {} (identifier: {})", action, maskIdentifier(identifier));
	}

	/**
	 * Assess risk without blocking (for logging/analytics).
	 * @param token the reCAPTCHA token
	 * @param action the action name
	 * @return the risk analysis result
	 */
	public RiskAnalysis assessRisk(String token, String action) {
		if (!enabled || recaptchaClient == null || !recaptchaClient.isAvailable()) {
			return RiskAnalysis.mock(1.0); // Return high score if not available
		}

		if (token == null || token.isEmpty()) {
			return RiskAnalysis.failed("NO_TOKEN");
		}

		return recaptchaClient.createAssessment(token, action);
	}

	/**
	 * Check if fraud detection is available.
	 */
	public boolean isAvailable() {
		return enabled && recaptchaClient != null && recaptchaClient.isAvailable();
	}

	/**
	 * Get the current threshold.
	 */
	public double getThreshold() {
		return threshold;
	}

	/**
	 * Mask identifier for secure logging.
	 */
	private String maskIdentifier(String identifier) {
		if (identifier == null || identifier.length() <= 4) {
			return "****";
		}
		if (identifier.contains("@")) {
			int atIndex = identifier.indexOf("@");
			if (atIndex <= 2) {
				return "***" + identifier.substring(atIndex);
			}
			return identifier.substring(0, 2) + "***" + identifier.substring(atIndex);
		}
		return identifier.substring(0, 2) + "***" + identifier.substring(identifier.length() - 2);
	}

}
