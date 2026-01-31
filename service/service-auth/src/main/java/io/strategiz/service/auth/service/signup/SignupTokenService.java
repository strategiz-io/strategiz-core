package io.strategiz.service.auth.service.signup;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.service.auth.exception.AuthErrors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for issuing and validating short-lived signup tokens.
 *
 * Signup tokens are PASETO v4.local tokens that prove email ownership without
 * creating an account. They are issued after OTP verification and consumed when
 * the user completes auth method registration (passkey/TOTP/SMS).
 *
 * The token carries the user's email, name, and a pre-generated userId so that
 * account creation can happen atomically with auth method setup.
 */
@Service
public class SignupTokenService {

	private static final Logger log = LoggerFactory.getLogger(SignupTokenService.class);

	private static final String EMPTY_FOOTER = "";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
	};

	private static final Duration SIGNUP_TOKEN_VALIDITY = Duration.ofMinutes(15);

	@Value("${auth.token.issuer:strategiz.io}")
	private String issuer;

	@Value("${auth.token.audience:strategiz}")
	private String audience;

	@Value("${spring.profiles.active:dev}")
	private String activeProfile;

	@Autowired(required = false)
	private SecretManager secretManager;

	private SecretKey signupKey;

	@PostConstruct
	public void init() {
		String env = activeProfile != null && activeProfile.contains("prod") ? "prod" : "dev";
		log.info("Initializing SignupTokenService for environment: {}", env);

		if (secretManager != null) {
			try {
				// Reuse the identity key for signup tokens (same security boundary)
				String keyPath = "tokens." + env + ".identity-key";
				String keyStr = secretManager.readSecret(keyPath);
				if (keyStr != null && !keyStr.isEmpty()) {
					signupKey = new SecretKey(Base64.getDecoder().decode(keyStr), Version.V4);
					log.info("SignupTokenService initialized with key from Vault");
				}
				else {
					log.error("Signup token key not found at path: {}", keyPath);
				}
			}
			catch (Exception e) {
				log.error("Failed to load signup token key from Vault: {}", e.getMessage());
			}
		}

		if (signupKey == null) {
			log.error("CRITICAL: SignupTokenService key not configured. Signup flow will not work.");
		}
	}

	/**
	 * Issue a signup token after successful email OTP verification.
	 * @param email The verified email address
	 * @param name The user's display name
	 * @return The signup token string
	 */
	public String issueSignupToken(String email, String name) {
		if (signupKey == null) {
			throw new StrategizException(AuthErrors.SIGNUP_FAILED, "Signup token service not configured");
		}

		String userId = UUID.randomUUID().toString();
		Instant now = Instant.now();
		Instant expiresAt = now.plus(SIGNUP_TOKEN_VALIDITY);

		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", userId);
		claims.put("email", email);
		claims.put("name", name);
		claims.put("iss", issuer);
		claims.put("aud", audience);
		claims.put("iat", now.getEpochSecond());
		claims.put("exp", expiresAt.getEpochSecond());
		claims.put("jti", UUID.randomUUID().toString());
		claims.put("purpose", "signup");
		claims.put("token_type", "signup");

		String payload = buildClaimsJson(claims);
		String token = Paseto.encrypt(signupKey, payload, EMPTY_FOOTER);

		log.info("Issued signup token for email: {} with userId: {} (expires in 15 min)", email, userId);
		return token;
	}

	/**
	 * Validate a signup token and extract its claims.
	 * @param token The signup token to validate
	 * @return The claims from the token
	 * @throws StrategizException if token is invalid or expired
	 */
	public SignupTokenClaims validateSignupToken(String token) {
		if (signupKey == null) {
			throw new StrategizException(AuthErrors.SIGNUP_FAILED, "Signup token service not configured");
		}

		try {
			String payload = Paseto.decrypt(signupKey, token, EMPTY_FOOTER);
			Map<String, Object> claims = objectMapper.readValue(payload, MAP_TYPE_REF);

			// Validate expiration
			Object expObj = claims.get("exp");
			if (expObj == null) {
				throw new StrategizException(AuthErrors.INVALID_TOKEN, "Signup token has no expiration");
			}
			long expEpoch = expObj instanceof Number ? ((Number) expObj).longValue() : Long.parseLong(expObj.toString());
			if (Instant.now().isAfter(Instant.ofEpochSecond(expEpoch))) {
				throw new StrategizException(AuthErrors.SESSION_EXPIRED,
						"Signup token has expired. Please re-verify your email.");
			}

			// Validate purpose
			String purpose = (String) claims.get("purpose");
			if (!"signup".equals(purpose)) {
				throw new StrategizException(AuthErrors.INVALID_TOKEN, "Invalid token purpose");
			}

			String userId = (String) claims.get("sub");
			String email = (String) claims.get("email");
			String name = (String) claims.get("name");
			long exp = expEpoch;

			return new SignupTokenClaims(userId, email, name, Instant.ofEpochSecond(exp));

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (PasetoException e) {
			log.warn("Invalid signup token: {}", e.getMessage());
			throw new StrategizException(AuthErrors.INVALID_TOKEN, "Invalid signup token");
		}
		catch (JsonProcessingException e) {
			log.error("Failed to parse signup token claims: {}", e.getMessage());
			throw new StrategizException(AuthErrors.INVALID_TOKEN, "Malformed signup token");
		}
	}

	private String buildClaimsJson(Map<String, Object> claims) {
		try {
			return objectMapper.writeValueAsString(claims);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize signup token claims", e);
		}
	}

	/**
	 * Claims extracted from a validated signup token.
	 */
	public record SignupTokenClaims(String userId, String email, String name, Instant expiresAt) {

		public long expiresInSeconds() {
			return Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
		}

	}

}
