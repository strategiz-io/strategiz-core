package io.strategiz.business.tokenauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.framework.authorization.error.AuthorizationErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.service.VaultSecretService;
import jakarta.annotation.PostConstruct;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provides PASETO token generation and validation functionality. Uses paseto4j library
 * for PASETO v4.local token creation and validation.
 *
 * <p>
 * Note: This class provides similar functionality to PasetoTokenIssuer (framework layer).
 * Consider using PasetoTokenIssuer and PasetoTokenValidator from framework modules for
 * new code.
 */
@Component
public class PasetoTokenProvider {

	private static final Logger log = LoggerFactory.getLogger(PasetoTokenProvider.class);

	private static final String EMPTY_FOOTER = "";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
	};

	/**
	 * Access token validity duration
	 */
	@Value("${auth.token.access.validity:30m}")
	private String accessTokenValidity;

	/**
	 * Refresh token validity duration
	 */
	@Value("${auth.token.refresh.validity:7d}")
	private String refreshTokenValidity;

	/**
	 * The audience claim for tokens
	 */
	@Value("${auth.token.audience:strategiz}")
	private String audience;

	/**
	 * The issuer claim for tokens
	 */
	@Value("${auth.token.issuer:strategiz.io}")
	private String issuer;

	/**
	 * The active Spring profile (dev, prod)
	 */
	@Value("${spring.profiles.active:dev}")
	private String activeProfile;

	@Autowired(required = false)
	private VaultSecretService vaultSecretService;

	private SecretKey identityKey; // For identity tokens (signup/profile creation)

	private SecretKey sessionKey; // For session tokens (authenticated users)

	/**
	 * Initialize the token provider with keys from Vault
	 */
	@PostConstruct
	public void init() {
		log.info("Initializing PASETO v4.local token provider with dual-key system");

		// Determine environment - handle comma-separated profiles (e.g.,
		// "prod,scheduler")
		String env = activeProfile != null && activeProfile.contains("prod") ? "prod" : "dev";
		log.info("Loading token keys for environment: {}", env);

		if (vaultSecretService != null) {
			try {
				// Load identity key from Vault
				String identityKeyPath = "tokens." + env + ".identity-key";
				log.info("Attempting to load identity key from Vault path: {}", identityKeyPath);
				String identityKeyStr = vaultSecretService.readSecret(identityKeyPath);
				if (identityKeyStr != null && !identityKeyStr.isEmpty()) {
					identityKey = new SecretKey(Base64.getDecoder().decode(identityKeyStr), Version.V4);
					log.info("Loaded identity token key from Vault for {}", env);
				}
				else {
					log.warn("Identity key is null or empty for path: {}", identityKeyPath);
				}

				// Load session key from Vault
				String sessionKeyPath = "tokens." + env + ".session-key";
				log.info("Attempting to load session key from Vault path: {}", sessionKeyPath);
				String sessionKeyStr = vaultSecretService.readSecret(sessionKeyPath);
				if (sessionKeyStr != null && !sessionKeyStr.isEmpty()) {
					sessionKey = new SecretKey(Base64.getDecoder().decode(sessionKeyStr), Version.V4);
					log.info("Loaded session token key from Vault for {}", env);
				}
				else {
					log.warn("Session key is null or empty for path: {}", sessionKeyPath);
				}
			}
			catch (Exception e) {
				log.error("Failed to load keys from Vault: {} - {}", e.getClass().getSimpleName(), e.getMessage());
			}
		}
		else {
			log.warn("VaultSecretService is null - cannot load keys from Vault");
		}

		// Require keys to be configured in Vault - no temporary keys
		if (identityKey == null || sessionKey == null) {
			log.error("CRITICAL: Token keys not found in Vault. Application cannot start.");
			log.error("Please configure keys in Vault at:");
			log.error("  - secret/strategiz/tokens/{}/identity-key", env);
			log.error("  - secret/strategiz/tokens/{}/session-key", env);
			throw new StrategizException(AuthorizationErrorDetails.CONFIGURATION_ERROR, "PasetoTokenProvider",
					"Token keys must be configured in Vault");
		}
	}

	/**
	 * Creates an identity token for signup/profile creation flow Limited scope,
	 * short-lived (30 minutes), uses identity key
	 * @param userId the user ID (or temporary ID for signup)
	 * @return the identity token string
	 */
	public String createIdentityToken(String userId) {
		log.info("=== PASETO TOKEN PROVIDER: createIdentityToken ===");
		log.info("PasetoTokenProvider.createIdentityToken - INPUT userId: [{}]", userId);
		log.info("PasetoTokenProvider.createIdentityToken - userId is UUID format: {}",
				userId != null && userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
		log.info("PasetoTokenProvider.createIdentityToken - USING DIRECT userId (no transformation)");

		Instant now = Instant.now();
		Instant expiresAt = now.plus(Duration.ofMinutes(30)); // Short-lived: 30 minutes
		String tokenId = UUID.randomUUID().toString();

		String payload = buildClaimsJson(Map.of("sub", userId, "iss", issuer, "aud", audience, "iat",
				now.getEpochSecond(), "exp", expiresAt.getEpochSecond(), "jti", tokenId, "type", "IDENTITY",
				"token_type", "identity", "scope", "profile:create", "acr", "0"));

		log.debug("Created identity token for user: {} with 30-minute expiry", userId);
		return Paseto.encrypt(identityKey, payload, EMPTY_FOOTER);
	}

	/**
	 * Creates a new refresh token for a user
	 * @param userId the user ID
	 * @return the token string
	 */
	public String createRefreshToken(String userId) {
		return createToken(userId, parseDuration(refreshTokenValidity), TokenType.REFRESH);
	}

	/**
	 * Creates a new token with specified parameters
	 * @param userId the user ID
	 * @param validity how long the token should be valid
	 * @param tokenType the type of token
	 * @param scopes optional scopes/roles
	 * @return the token string
	 */
	public String createToken(String userId, Duration validity, TokenType tokenType, String... scopes) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(validity);
		String tokenId = UUID.randomUUID().toString();

		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", userId);
		claims.put("iss", issuer);
		claims.put("aud", audience);
		claims.put("iat", now.getEpochSecond());
		claims.put("exp", expiresAt.getEpochSecond());
		claims.put("jti", tokenId);
		claims.put("type", tokenType.name());

		if (scopes != null && scopes.length > 0) {
			claims.put("scope", String.join(" ", scopes));
		}

		return Paseto.encrypt(sessionKey, buildClaimsJson(claims), EMPTY_FOOTER);
	}

	/**
	 * Creates a token with full Strategiz claims structure for authentication flows
	 * @param userId the internal user ID
	 * @param authenticationMethods list of authentication methods used
	 * @param acr authentication context reference ("0", "1", "2", "3")
	 * @param validity how long the token should be valid
	 * @param demoMode the user's demo mode (true for demo, false for live)
	 * @return the token string
	 */
	public String createAuthenticationToken(String userId, List<String> authenticationMethods, String acr,
			Duration validity, Boolean demoMode) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(validity);
		String tokenId = UUID.randomUUID().toString();

		// Convert auth methods to numeric AMR
		List<Integer> amr = encodeAuthenticationMethods(authenticationMethods);

		// Calculate scopes based on user entitlements, not ACR
		String scope = calculateUserScopes(userId);

		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", userId);
		claims.put("iss", issuer);
		claims.put("aud", audience);
		claims.put("iat", now.getEpochSecond());
		claims.put("exp", expiresAt.getEpochSecond());
		claims.put("jti", tokenId);
		claims.put("amr", amr);
		claims.put("acr", acr);
		claims.put("auth_time", now.getEpochSecond());
		claims.put("scope", scope);
		claims.put("type", "ACCESS");
		claims.put("token_type", "session");
		claims.put("demoMode", demoMode != null ? demoMode : true);

		return Paseto.encrypt(sessionKey, buildClaimsJson(claims), EMPTY_FOOTER);
	}

	/**
	 * Creates a token with automatic ACR calculation based on authentication methods
	 * @param userId the internal user ID
	 * @param authenticationMethods list of authentication methods used
	 * @param isPartialAuth whether this is partial authentication (signup incomplete)
	 * @param validity how long the token should be valid
	 * @return the token string
	 */
	public String createAuthenticationToken(String userId, List<String> authenticationMethods, boolean isPartialAuth,
			Duration validity) {
		String acr = calculateAcr(authenticationMethods, isPartialAuth);
		return createAuthenticationToken(userId, authenticationMethods, acr, validity, true);
	}

	/**
	 * Updates token claims when new authenticator is added during session This creates a
	 * new token with updated ACR based on additional authentication
	 * @param currentToken the current token
	 * @param additionalAuthMethods new authentication methods completed
	 * @param validity how long the new token should be valid
	 * @return new token with updated claims
	 * @throws PasetoException if current token is invalid
	 */
	public String updateTokenWithAdditionalAuth(String currentToken, List<String> additionalAuthMethods,
			Duration validity) throws PasetoException {
		// Parse current token to get existing claims
		Map<String, Object> currentClaims = parseToken(currentToken);
		String userId = (String) currentClaims.get("sub");

		// Get current authentication methods from AMR
		List<Integer> currentAmr = new ArrayList<>();
		Object amrObj = currentClaims.get("amr");
		if (amrObj instanceof List<?>) {
			List<?> amrList = (List<?>) amrObj;
			for (Object item : amrList) {
				if (item instanceof Integer) {
					currentAmr.add((Integer) item);
				}
				else if (item instanceof Number) {
					currentAmr.add(((Number) item).intValue());
				}
			}
		}
		List<String> currentMethods = decodeAuthenticationMethods(currentAmr);

		// Combine current and additional methods
		List<String> combinedMethods = new ArrayList<>(currentMethods);
		combinedMethods.addAll(additionalAuthMethods);

		// Calculate new ACR - assume full authentication now
		String newAcr = calculateAcr(combinedMethods, false);

		// Preserve demo mode from current token
		Boolean demoMode = (Boolean) currentClaims.getOrDefault("demoMode", true);

		return createAuthenticationToken(userId, combinedMethods, newAcr, validity, demoMode);
	}

	/**
	 * Decodes numeric AMR back to authentication method names
	 * @param amr list of numeric method identifiers
	 * @return list of authentication method names
	 */
	private List<String> decodeAuthenticationMethods(List<Integer> amr) {
		if (amr == null || amr.isEmpty()) {
			return new ArrayList<>();
		}

		// Reverse mapping of authentication methods
		Map<Integer, String> methodMap = Map.of(1, "password", 2, "sms_otp", 3, "passkeys", 4, "totp", 5, "email_otp",
				6, "backup_codes");

		return amr.stream().filter(methodMap::containsKey).map(methodMap::get).collect(Collectors.toList());
	}

	/**
	 * Encodes authentication methods to numeric list for obfuscation
	 * @param authMethods list of authentication method names
	 * @return list of numeric method identifiers
	 */
	private List<Integer> encodeAuthenticationMethods(List<String> authMethods) {
		if (authMethods == null || authMethods.isEmpty()) {
			return new ArrayList<>();
		}

		// Authentication method mapping
		Map<String, Integer> methodMap = Map.of("password", 1, "sms_otp", 2, "passkeys", 3, "totp", 4, "email_otp", 5,
				"backup_codes", 6);

		return authMethods.stream().filter(methodMap::containsKey).map(methodMap::get).collect(Collectors.toList());
	}

	/**
	 * Calculates Authentication Context Reference (ACR) based on authentication methods
	 * ACR values: - "0": No authentication (partial/signup in progress) - "1":
	 * Single-factor authentication - "2": Multi-factor authentication (standard MFA) -
	 * "3": Strong multi-factor (hardware key + another factor)
	 * @param authenticationMethods list of authentication methods completed
	 * @param isPartialAuth whether this is partial authentication (signup incomplete)
	 * @return ACR value as string
	 */
	public String calculateAcr(List<String> authenticationMethods, boolean isPartialAuth) {
		if (isPartialAuth || authenticationMethods == null || authenticationMethods.isEmpty()) {
			return "0"; // No/partial authentication
		}

		// Hardware-based MFA (highest level)
		if (authenticationMethods.contains("passkeys") && authenticationMethods.size() > 1) {
			return "3"; // Strong MFA with hardware authenticator
		}

		// Standard MFA or passkey alone (passkeys are inherently strong)
		if (authenticationMethods.size() >= 2 || authenticationMethods.contains("passkeys")) {
			return "2"; // Multi-factor or strong single factor
		}

		// Single factor authentication
		return "1"; // Basic authentication
	}

	/**
	 * Calculates user scopes based on entitlements and permissions This should be
	 * determined by user roles/permissions, not authentication strength
	 * @param userId the user ID for fetching permissions
	 * @return space-separated scope string
	 */
	private String calculateUserScopes(String userId) {
		// TODO: Fetch actual user permissions from database/service
		// For now, return standard user scopes
		List<String> scopes = Arrays.asList("read:profile", "write:profile", "read:portfolio", "write:portfolio",
				"read:positions", "write:positions", "read:market_data", "read:watchlists", "write:watchlists",
				"read:trades", "write:trades", "read:strategies", "write:strategies", "read:settings", "write:settings",
				"read:auth_methods", "write:auth_methods");

		return String.join(" ", scopes);
	}

	/**
	 * Parses and validates a token
	 * @param token the token to validate
	 * @return the claims from the token
	 * @throws PasetoException if the token is invalid or expired
	 */
	public Map<String, Object> parseToken(String token) throws PasetoException {
		// Try session key first (most common case)
		try {
			return parseWithKey(token, sessionKey);
		}
		catch (PasetoException e) {
			// If session key fails, try identity key
			try {
				Map<String, Object> claims = parseWithKey(token, identityKey);
				// Verify it's actually an identity token
				String tokenType = (String) claims.get("token_type");
				if ("identity".equals(tokenType) || "recovery".equals(tokenType)) {
					return claims;
				}
				throw new PasetoException("Token validation failed - not an identity token");
			}
			catch (PasetoException identityException) {
				// Both keys failed, throw original exception
				throw e;
			}
		}
	}

	/**
	 * Parse token with a specific key and validate expiration.
	 */
	private Map<String, Object> parseWithKey(String token, SecretKey key) throws PasetoException {
		try {
			String payload = Paseto.decrypt(key, token, EMPTY_FOOTER);
			Map<String, Object> claims = objectMapper.readValue(payload, MAP_TYPE_REF);
			validateExpiration(claims);
			return claims;
		}
		catch (JsonProcessingException e) {
			throw new PasetoException("Failed to parse token claims: " + e.getMessage());
		}
	}

	/**
	 * Validates token expiration.
	 */
	private void validateExpiration(Map<String, Object> claims) throws PasetoException {
		Object expObj = claims.get("exp");
		if (expObj == null) {
			throw new PasetoException("Token has no expiration claim");
		}

		long expEpoch;
		if (expObj instanceof Long) {
			expEpoch = (Long) expObj;
		}
		else if (expObj instanceof Number) {
			expEpoch = ((Number) expObj).longValue();
		}
		else {
			throw new PasetoException("Invalid expiration claim format");
		}

		if (Instant.now().isAfter(Instant.ofEpochSecond(expEpoch))) {
			throw new PasetoException("Token has expired");
		}
	}

	/**
	 * Gets the user ID from a token
	 * @param token the token
	 * @return the user ID
	 * @throws PasetoException if the token is invalid
	 */
	public String getUserIdFromToken(String token) throws PasetoException {
		return (String) parseToken(token).get("sub");
	}

	/**
	 * Validates if a token is an access token
	 * @param token the token to validate
	 * @return true if valid
	 */
	public boolean isValidAccessToken(String token) {
		try {
			Map<String, Object> claims = parseToken(token);
			String tokenType = (String) claims.get("token_type");
			log.debug("Token claims - token_type: {}, type: {}", tokenType, claims.get("type"));
			// Session tokens are valid access tokens
			boolean valid = "session".equals(tokenType) || TokenType.ACCESS.name().equals(claims.get("type"));
			if (!valid) {
				log.warn("Token is not a valid access token - token_type: {}, type: {}", tokenType, claims.get("type"));
			}
			return valid;
		}
		catch (PasetoException e) {
			log.warn("Failed to parse token - error: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Validates if a token is an identity token
	 * @param token the token to validate
	 * @return true if valid identity token
	 */
	public boolean isValidIdentityToken(String token) {
		try {
			Map<String, Object> claims = parseToken(token);
			String tokenType = (String) claims.get("token_type");
			return "identity".equals(tokenType);
		}
		catch (PasetoException e) {
			log.debug("Invalid identity token: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Validates if a token is a refresh token
	 * @param token the token to validate
	 * @return true if valid
	 */
	public boolean isValidRefreshToken(String token) {
		try {
			Map<String, Object> claims = parseToken(token);
			String tokenType = (String) claims.get("type");
			return TokenType.REFRESH.name().equals(tokenType);
		}
		catch (PasetoException e) {
			log.error("Invalid token: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Builds JSON string from claims map.
	 */
	private String buildClaimsJson(Map<String, Object> claims) {
		try {
			return objectMapper.writeValueAsString(claims);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize token claims", e);
		}
	}

	/**
	 * Parses a duration string like "30m", "1h", "7d"
	 * @param duration the duration string
	 * @return the Duration object
	 */
	private Duration parseDuration(String duration) {
		if (duration == null || duration.isEmpty()) {
			return Duration.ofMinutes(30); // Default to 30 minutes
		}

		String value = duration.substring(0, duration.length() - 1);
		char unit = duration.charAt(duration.length() - 1);

		try {
			long amount = Long.parseLong(value);
			return switch (Character.toLowerCase(unit)) {
				case 's' -> Duration.ofSeconds(amount);
				case 'm' -> Duration.ofMinutes(amount);
				case 'h' -> Duration.ofHours(amount);
				case 'd' -> Duration.ofDays(amount);
				default -> Duration.ofMinutes(30);
			};
		}
		catch (NumberFormatException e) {
			log.error("Invalid duration format: {}", duration);
			return Duration.ofMinutes(30); // Default to 30 minutes
		}
	}

	/**
	 * Enum for token types
	 */
	public enum TokenType {

		ACCESS, REFRESH

	}

}
