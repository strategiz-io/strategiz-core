package io.strategiz.client.robinhood;

import io.strategiz.client.robinhood.error.RobinhoodErrors;
import io.strategiz.client.robinhood.model.RobinhoodChallenge;
import io.strategiz.client.robinhood.model.RobinhoodLoginResult;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client for handling Robinhood OAuth operations.
 *
 * Robinhood uses a password grant flow (not authorization code flow): 1. User provides
 * username/password 2. Request to /oauth2/token/ - may fail with 400 if MFA required 3.
 * If MFA required, Robinhood sends SMS/email with code 4. Submit challenge response to
 * /challenge/{id}/respond/ 5. Retry token request with challenge response header
 *
 * Note: This is an unofficial API integration. Robinhood may change it at any time.
 */
@Component
public class RobinhoodOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(RobinhoodOAuthClient.class);

	private static final String BASE_URL = "https://api.robinhood.com";

	private static final String TOKEN_URL = BASE_URL + "/oauth2/token/";

	private static final String CHALLENGE_URL = BASE_URL + "/challenge/%s/respond/";

	// Known Robinhood client IDs (from unofficial documentation)
	private static final String DEFAULT_CLIENT_ID = "c82SH0WZOsabOXGP2sxqcj34FxkvfnWRZBKlBjFS";

	private final RestTemplate restTemplate;

	@Value("${robinhood.client-id:" + DEFAULT_CLIENT_ID + "}")
	private String clientId;

	public RobinhoodOAuthClient(@Qualifier("robinhoodRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("RobinhoodOAuthClient initialized");
	}

	/**
	 * Initiate login with username and password. This will either succeed with tokens or
	 * trigger MFA.
	 * @param username Robinhood username (email)
	 * @param password Robinhood password
	 * @param challengeType MFA challenge type - "sms" or "email"
	 * @return Login result with tokens or MFA challenge info
	 */
	public RobinhoodLoginResult login(String username, String password, String challengeType) {
		String deviceToken = generateDeviceToken();
		return login(username, password, challengeType, deviceToken, null, null);
	}

	/**
	 * Initiate login with all parameters.
	 * @param username Robinhood username (email)
	 * @param password Robinhood password
	 * @param challengeType MFA challenge type - "sms" or "email"
	 * @param deviceToken Device token for this session
	 * @param mfaCode MFA code if responding to challenge
	 * @param challengeId Challenge ID if responding to challenge
	 * @return Login result with tokens or MFA challenge info
	 */
	public RobinhoodLoginResult login(String username, String password, String challengeType, String deviceToken,
			String mfaCode, String challengeId) {
		try {
			log.info("Attempting Robinhood login for user: {}", maskEmail(username));

			HttpHeaders headers = buildHeaders();

			// Add challenge response header if we're responding to MFA
			if (challengeId != null) {
				headers.set("X-ROBINHOOD-CHALLENGE-RESPONSE-ID", challengeId);
			}

			Map<String, Object> body = new HashMap<>();
			body.put("grant_type", "password");
			body.put("scope", "internal");
			body.put("client_id", clientId);
			body.put("device_token", deviceToken);
			body.put("username", username);
			body.put("password", password);

			// Add MFA code if provided
			if (mfaCode != null && !mfaCode.isEmpty()) {
				body.put("mfa_code", mfaCode);
			}

			// Add challenge type for initial request
			if (challengeType != null && challengeId == null) {
				body.put("challenge_type", challengeType);
			}

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				Map<String, Object> responseBody = response.getBody();

				String accessToken = (String) responseBody.get("access_token");
				String refreshToken = (String) responseBody.get("refresh_token");
				Integer expiresIn = (Integer) responseBody.get("expires_in");

				if (accessToken != null) {
					log.info("Robinhood login successful for user: {}", maskEmail(username));
					return RobinhoodLoginResult.success(accessToken, refreshToken, expiresIn);
				}
			}

			return RobinhoodLoginResult.error("Unexpected response from Robinhood", "UNEXPECTED_RESPONSE");

		}
		catch (RestClientResponseException e) {
			return handleLoginError(e, deviceToken, challengeType);
		}
		catch (Exception e) {
			log.error("Unexpected error during Robinhood login", e);
			return RobinhoodLoginResult.error("Login failed. Please try again.", "UNKNOWN_ERROR");
		}
	}

	/**
	 * Respond to MFA challenge with the code sent via SMS/email.
	 * @param challengeId The challenge ID from the initial login attempt
	 * @param code The MFA code received via SMS/email
	 * @return true if challenge was validated
	 */
	public boolean respondToChallenge(String challengeId, String code) {
		try {
			log.info("Responding to Robinhood MFA challenge: {}", challengeId);

			HttpHeaders headers = buildHeaders();

			Map<String, Object> body = new HashMap<>();
			body.put("response", code);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			String url = String.format(CHALLENGE_URL, challengeId);
			ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				Map<String, Object> responseBody = response.getBody();
				String status = (String) responseBody.get("status");

				if ("validated".equalsIgnoreCase(status)) {
					log.info("MFA challenge validated successfully");
					return true;
				}
			}

			log.warn("MFA challenge validation failed");
			return false;

		}
		catch (RestClientResponseException e) {
			log.error("MFA challenge response failed with status {}: {}", e.getStatusCode(),
					e.getResponseBodyAsString());
			return false;
		}
		catch (Exception e) {
			log.error("Error responding to MFA challenge", e);
			return false;
		}
	}

	/**
	 * Complete login after MFA challenge has been validated.
	 * @param username Robinhood username
	 * @param password Robinhood password
	 * @param deviceToken Device token from initial login
	 * @param challengeId Validated challenge ID
	 * @return Login result with tokens
	 */
	public RobinhoodLoginResult completeLoginAfterMfa(String username, String password, String deviceToken,
			String challengeId) {
		return login(username, password, null, deviceToken, null, challengeId);
	}

	/**
	 * Refresh access token using refresh token.
	 * @param refreshToken The refresh token
	 * @return Map containing new access_token, refresh_token, expires_in
	 */
	public Map<String, Object> refreshAccessToken(String refreshToken) {
		try {
			log.debug("Refreshing Robinhood access token");

			HttpHeaders headers = buildHeaders();

			Map<String, Object> body = new HashMap<>();
			body.put("grant_type", "refresh_token");
			body.put("scope", "internal");
			body.put("client_id", clientId);
			body.put("refresh_token", refreshToken);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.info("Successfully refreshed Robinhood access token");
				return response.getBody();
			}

			throw new StrategizException(RobinhoodErrors.ROBINHOOD_TOKEN_EXPIRED, "Failed to refresh access token");

		}
		catch (RestClientResponseException e) {
			log.error("Robinhood token refresh failed with status {}: {}", e.getStatusCode(),
					e.getResponseBodyAsString());
			throw new StrategizException(RobinhoodErrors.ROBINHOOD_TOKEN_EXPIRED,
					"Token refresh failed: " + e.getResponseBodyAsString());
		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error during Robinhood token refresh", e);
			throw new StrategizException(RobinhoodErrors.ROBINHOOD_API_ERROR,
					"Failed to refresh access token: " + e.getMessage());
		}
	}

	/**
	 * Handle login error responses from Robinhood. Parses the error to determine if MFA
	 * is required or if it's a real error.
	 */
	private RobinhoodLoginResult handleLoginError(RestClientResponseException e, String deviceToken,
			String challengeType) {
		int statusCode = e.getStatusCode().value();
		String responseBody = e.getResponseBodyAsString();

		log.debug("Robinhood login returned status {}: {}", statusCode, responseBody);

		// Parse the response body once for use in all checks
		Map<String, Object> errorResponse = parseJsonResponse(responseBody);

		// 400 status often means MFA is required
		if (statusCode == 400) {
			try {
				// Check for challenge in response
				if (errorResponse.containsKey("challenge")) {
					Map<String, Object> challengeMap = (Map<String, Object>) errorResponse.get("challenge");
					RobinhoodChallenge challenge = new RobinhoodChallenge();
					challenge.setId((String) challengeMap.get("id"));
					challenge.setType((String) challengeMap.get("type"));
					challenge.setStatus((String) challengeMap.get("status"));

					log.info("MFA challenge required, type: {}", challenge.getType());
					return RobinhoodLoginResult.mfaRequired(challenge, challenge.getType(), deviceToken);
				}

				// Check for device approval / push notification requirement.
				// Robinhood may return {"detail":"Unable to log in with provided
				// credentials."}
				// while simultaneously sending a push notification to the mobile app.
				// Detect this by checking for common device/push-related patterns.
				if (errorResponse.containsKey("detail")) {
					String detail = (String) errorResponse.get("detail");
					if (detail != null) {
						String detailLower = detail.toLowerCase();
						if (detailLower.contains("device") || detailLower.contains("unable to log in")
								|| detailLower.contains("push")) {
							log.info("Device approval / push notification required (detail: {})", detail);
							return RobinhoodLoginResult.deviceApprovalRequired(deviceToken);
						}
					}
				}

				// Check for mfa_required flag
				Object mfaRequired = errorResponse.get("mfa_required");
				if (Boolean.TRUE.equals(mfaRequired)) {
					log.info("MFA required (mfa_required flag)");
					return RobinhoodLoginResult.mfaRequired(null, challengeType, deviceToken);
				}

			}
			catch (Exception parseError) {
				log.warn("Could not parse Robinhood error response: {}", parseError.getMessage());
			}
		}

		// Handle other errors
		if (statusCode == 401) {
			return RobinhoodLoginResult.error("Invalid credentials", "INVALID_CREDENTIALS");
		}

		if (statusCode == 429) {
			return RobinhoodLoginResult.error("Rate limited - please try again later", "RATE_LIMITED");
		}

		// Extract a human-readable message from the response, falling back to a generic
		// message
		String errorMessage = extractErrorMessage(errorResponse, responseBody);
		return RobinhoodLoginResult.error(errorMessage, "HTTP_" + statusCode);
	}

	/**
	 * Extract a human-readable error message from Robinhood's error response. Prefers the
	 * "detail" field if present, otherwise falls back to a generic message.
	 */
	private String extractErrorMessage(Map<String, Object> errorResponse, String rawResponseBody) {
		if (errorResponse != null && errorResponse.containsKey("detail")) {
			Object detail = errorResponse.get("detail");
			if (detail instanceof String && !((String) detail).isEmpty()) {
				return (String) detail;
			}
		}
		if (errorResponse != null && errorResponse.containsKey("error")) {
			Object error = errorResponse.get("error");
			if (error instanceof String && !((String) error).isEmpty()) {
				return (String) error;
			}
		}
		return "Login failed. Please try again.";
	}

	/**
	 * Build standard headers for Robinhood API requests.
	 */
	private HttpHeaders buildHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Accept", "*/*");
		headers.set("Accept-Encoding", "gzip, deflate");
		headers.set("Accept-Language", "en-US,en;q=0.9");
		headers.set("X-Robinhood-API-Version", "1.431.4");
		headers.set("Origin", "https://robinhood.com");
		headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
		return headers;
	}

	/**
	 * Generate a unique device token for this session.
	 */
	private String generateDeviceToken() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Parse JSON response string to Map.
	 */
	private Map<String, Object> parseJsonResponse(String json) {
		try {
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			return mapper.readValue(json, Map.class);
		}
		catch (Exception e) {
			return new HashMap<>();
		}
	}

	/**
	 * Mask email for logging.
	 */
	private String maskEmail(String email) {
		if (email == null || email.length() < 5) {
			return "***";
		}
		int atIndex = email.indexOf('@');
		if (atIndex > 2) {
			return email.substring(0, 2) + "***" + email.substring(atIndex);
		}
		return "***" + email.substring(Math.max(0, email.length() - 4));
	}

}
