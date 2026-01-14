package io.strategiz.client.recaptcha;

import com.google.cloud.recaptchaenterprise.v1.RecaptchaEnterpriseServiceClient;
import com.google.recaptchaenterprise.v1.Assessment;
import com.google.recaptchaenterprise.v1.CreateAssessmentRequest;
import com.google.recaptchaenterprise.v1.Event;
import com.google.recaptchaenterprise.v1.ProjectName;
import com.google.recaptchaenterprise.v1.RiskAnalysis.ClassificationReason;
import com.google.recaptchaenterprise.v1.TokenProperties;
import io.strategiz.client.recaptcha.model.RiskAnalysis;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client for Google reCAPTCHA Enterprise.
 *
 * Verifies reCAPTCHA tokens and returns risk scores for fraud detection. Uses the official
 * Google Cloud reCAPTCHA Enterprise SDK.
 */
@Component
@ConditionalOnProperty(name = "recaptcha.enabled", havingValue = "true", matchIfMissing = false)
public class RecaptchaClient {

	private static final Logger logger = LoggerFactory.getLogger(RecaptchaClient.class);

	private final RecaptchaConfig config;

	private RecaptchaEnterpriseServiceClient client;

	private boolean initialized = false;

	public RecaptchaClient(RecaptchaConfig config) {
		this.config = config;
	}

	@PostConstruct
	public void init() {
		if (config.isConfigured() && !config.isMockEnabled()) {
			try {
				client = RecaptchaEnterpriseServiceClient.create();
				initialized = true;
				logger.info("reCAPTCHA Enterprise client initialized successfully for project: {}",
						config.getProjectId());
			}
			catch (IOException e) {
				logger.error("Failed to initialize reCAPTCHA Enterprise client: {}", e.getMessage());
				initialized = false;
			}
		}
		else if (config.isMockEnabled()) {
			logger.info("reCAPTCHA Enterprise client running in mock mode");
			initialized = true;
		}
		else {
			logger.warn("reCAPTCHA Enterprise client not configured - fraud detection will be disabled");
		}
	}

	@PreDestroy
	public void shutdown() {
		if (client != null) {
			try {
				client.close();
				logger.info("reCAPTCHA Enterprise client shutdown complete");
			}
			catch (Exception e) {
				logger.warn("Error during reCAPTCHA client shutdown: {}", e.getMessage());
			}
		}
	}

	/**
	 * Create an assessment for a reCAPTCHA token.
	 * @param token the reCAPTCHA token from the frontend
	 * @param expectedAction the expected action (e.g., "signup", "login")
	 * @return RiskAnalysis containing score and classification
	 */
	public RiskAnalysis createAssessment(String token, String expectedAction) {
		if (!isAvailable()) {
			logger.warn("reCAPTCHA client not available, returning failed assessment");
			return RiskAnalysis.failed("CLIENT_NOT_AVAILABLE");
		}

		// Mock mode for development
		if (config.isMockEnabled()) {
			logger.info("[MOCK reCAPTCHA] Token: {}, Action: {}", truncateToken(token), expectedAction);
			return RiskAnalysis.mock(0.9); // Return high score in mock mode
		}

		try {
			// Build the event with the token
			Event event = Event.newBuilder().setSiteKey(config.getSiteKey()).setToken(token).build();

			// Build the assessment request
			CreateAssessmentRequest request = CreateAssessmentRequest.newBuilder()
				.setParent(ProjectName.of(config.getProjectId()).toString())
				.setAssessment(Assessment.newBuilder().setEvent(event).build())
				.build();

			// Execute the assessment
			Assessment assessment = client.createAssessment(request);

			// Extract token properties
			TokenProperties tokenProperties = assessment.getTokenProperties();

			if (!tokenProperties.getValid()) {
				logger.warn("reCAPTCHA token invalid. Reason: {}", tokenProperties.getInvalidReason());
				return RiskAnalysis.builder()
					.valid(false)
					.score(0.0)
					.reasons(List.of(tokenProperties.getInvalidReason().name()))
					.build();
			}

			// Verify action matches
			if (!expectedAction.equals(tokenProperties.getAction())) {
				logger.warn("reCAPTCHA action mismatch. Expected: {}, Got: {}", expectedAction,
						tokenProperties.getAction());
				return RiskAnalysis.builder()
					.valid(false)
					.score(0.0)
					.tokenAction(tokenProperties.getAction())
					.reasons(List.of("ACTION_MISMATCH"))
					.build();
			}

			// Extract risk analysis
			com.google.recaptchaenterprise.v1.RiskAnalysis riskAnalysis = assessment.getRiskAnalysis();
			double score = riskAnalysis.getScore();
			List<String> reasons = riskAnalysis.getReasonsList()
				.stream()
				.map(ClassificationReason::name)
				.collect(Collectors.toList());

			logger.info("reCAPTCHA assessment complete. Score: {}, Action: {}", score, tokenProperties.getAction());

			return RiskAnalysis.builder()
				.valid(true)
				.score(score)
				.tokenAction(tokenProperties.getAction())
				.reasons(reasons)
				.assessmentName(assessment.getName())
				.build();
		}
		catch (Exception e) {
			logger.error("Failed to create reCAPTCHA assessment: {}", e.getMessage(), e);
			return RiskAnalysis.failed("ASSESSMENT_ERROR: " + e.getMessage());
		}
	}

	/**
	 * Quick check if a token is likely from a human.
	 * @param token the reCAPTCHA token
	 * @param expectedAction the expected action
	 * @return true if score >= threshold
	 */
	public boolean isLikelyHuman(String token, String expectedAction) {
		RiskAnalysis analysis = createAssessment(token, expectedAction);
		return analysis.isLikelyHuman(config.getThreshold());
	}

	/**
	 * Check if the client is available for use.
	 */
	public boolean isAvailable() {
		return config.isEnabled() && (initialized || config.isMockEnabled());
	}

	/**
	 * Get the configured score threshold.
	 */
	public double getThreshold() {
		return config.getThreshold();
	}

	/**
	 * Truncate token for logging.
	 */
	private String truncateToken(String token) {
		if (token == null || token.length() <= 10) {
			return "****";
		}
		return token.substring(0, 10) + "...";
	}

}
