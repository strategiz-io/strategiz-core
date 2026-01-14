package io.strategiz.framework.token.issuer;

import io.strategiz.framework.secrets.service.VaultSecretService;
import io.strategiz.framework.token.util.PasetoClaimsUtil;
import jakarta.annotation.PostConstruct;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PASETO Token Issuer - Responsible for TOKEN CREATION only.
 * All token validation is handled by framework-authorization.
 *
 * <p>This class provides methods to create various types of PASETO v4.local tokens:</p>
 * <ul>
 *   <li>Identity tokens - For signup/profile creation flow</li>
 *   <li>Authentication tokens - For authenticated sessions with ACR/AMR</li>
 *   <li>Refresh tokens - For token refresh flow</li>
 * </ul>
 *
 * <p>Uses paseto4j library for PASETO v4.local token creation.</p>
 */
@Component
public class PasetoTokenIssuer {

	private static final Logger log = LoggerFactory.getLogger(PasetoTokenIssuer.class);

	private static final String EMPTY_FOOTER = "";

	@Value("${auth.token.access.validity:30m}")
	private String accessTokenValidity;

	@Value("${auth.token.refresh.validity:7d}")
	private String refreshTokenValidity;

	@Value("${auth.token.audience:strategiz}")
	private String audience;

	@Value("${auth.token.issuer:strategiz.io}")
	private String issuer;

	@Value("${spring.profiles.active:dev}")
	private String activeProfile;

	@Autowired(required = false)
	private VaultSecretService vaultSecretService;

	private SecretKey identityKey; // For identity tokens (signup/profile creation)

	private SecretKey sessionKey; // For session tokens (authenticated users)

	/**
	 * Initialize the token issuer with keys from Vault
	 */
	@PostConstruct
	public void init() {
		log.info("Initializing PasetoTokenIssuer with dual-key system (v4.local)");

		// Handle comma-separated profiles (e.g., "prod,scheduler")
		String env = activeProfile != null && activeProfile.contains("prod") ? "prod" : "dev";
		log.info("Loading token keys for environment: {}", env);

		if (vaultSecretService != null) {
			try {
				// Load identity key from Vault
				String identityKeyPath = "tokens." + env + ".identity-key";
				String identityKeyStr = vaultSecretService.readSecret(identityKeyPath);
				if (identityKeyStr != null && !identityKeyStr.isEmpty()) {
					identityKey = new SecretKey(Base64.getDecoder().decode(identityKeyStr), Version.V4);
					log.info("Loaded identity token key from Vault for {}", env);
				}

				// Load session key from Vault
				String sessionKeyPath = "tokens." + env + ".session-key";
				String sessionKeyStr = vaultSecretService.readSecret(sessionKeyPath);
				if (sessionKeyStr != null && !sessionKeyStr.isEmpty()) {
					sessionKey = new SecretKey(Base64.getDecoder().decode(sessionKeyStr), Version.V4);
					log.info("Loaded session token key from Vault for {}", env);
				}
			}
			catch (Exception e) {
				log.error("Failed to load keys from Vault: {} - {}", e.getClass().getSimpleName(), e.getMessage());
			}
		}

		if (identityKey == null || sessionKey == null) {
			log.error("CRITICAL: Token keys not found in Vault. Token issuance will fail until keys are loaded.");
			log.error("Check Vault connectivity and ensure keys exist at: tokens.{}.identity-key and tokens.{}.session-key",
					env, env);
			// Don't throw - allow application to start but token operations will fail
			// gracefully
		}
		else {
			log.info("PasetoTokenIssuer initialized successfully with keys for environment: {}", env);
		}
	}

	/**
	 * Creates an identity token for signup/profile creation flow. Limited scope,
	 * short-lived (30 minutes), uses identity key.
	 * @param userId the user ID (or temporary ID for signup)
	 * @return the identity token string
	 */
	public String createIdentityToken(String userId) {
		log.info("PasetoTokenIssuer.createIdentityToken - userId (sub claim): {}", userId);
		Instant now = Instant.now();
		Instant expiresAt = now.plus(Duration.ofMinutes(30));
		String tokenId = UUID.randomUUID().toString();

		String payload = PasetoClaimsUtil.builder()
			.subject(userId)
			.issuer(issuer)
			.audience(audience)
			.issuedAt(now)
			.expiration(expiresAt)
			.tokenId(tokenId)
			.claim("type", "IDENTITY")
			.claim("token_type", "identity")
			.claim("scope", "profile:create")
			.claim("acr", "0")
			.buildJson();

		log.info("Created identity token for user: {} with 30-minute expiry", userId);
		return Paseto.encrypt(identityKey, payload, EMPTY_FOOTER);
	}

	/**
	 * Creates a recovery token for account recovery flow. Limited scope, short-lived
	 * (15 minutes), uses identity key.
	 *
	 * <p>
	 * Recovery tokens allow users to:
	 * </p>
	 * <ul>
	 * <li>Disable MFA methods</li>
	 * <li>Unlink OAuth providers</li>
	 * <li>Set up new passkey</li>
	 * <li>Complete recovery (issues real session token)</li>
	 * </ul>
	 * @param userId the user ID
	 * @param recoveryId the recovery request ID for tracking
	 * @return the recovery token string
	 */
	public String createRecoveryToken(String userId, String recoveryId) {
		log.info("PasetoTokenIssuer.createRecoveryToken - userId: {}, recoveryId: {}", userId, recoveryId);
		Instant now = Instant.now();
		Instant expiresAt = now.plus(Duration.ofMinutes(15));
		String tokenId = UUID.randomUUID().toString();

		String payload = PasetoClaimsUtil.builder()
			.subject(userId)
			.issuer(issuer)
			.audience(audience)
			.issuedAt(now)
			.expiration(expiresAt)
			.tokenId(tokenId)
			.claim("type", "RECOVERY")
			.claim("token_type", "recovery")
			.claim("scope", "account:recover")
			.claim("acr", "0") // Unauthenticated - obtained via recovery, not normal
								// login
			.claim("recovery_id", recoveryId)
			.buildJson();

		log.info("Created recovery token for user: {} with 15-minute expiry", userId);
		return Paseto.encrypt(identityKey, payload, EMPTY_FOOTER);
	}

	/**
	 * Creates a new refresh token for a user.
	 * @param userId the user ID
	 * @return the token string
	 */
	public String createRefreshToken(String userId) {
		return createToken(userId, parseDuration(refreshTokenValidity), TokenType.REFRESH);
	}

	/**
	 * Creates a new token with specified parameters.
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

		var builder = PasetoClaimsUtil.builder()
			.subject(userId)
			.issuer(issuer)
			.audience(audience)
			.issuedAt(now)
			.expiration(expiresAt)
			.tokenId(tokenId)
			.claim("type", tokenType.name());

		if (scopes != null && scopes.length > 0) {
			builder.claim("scope", String.join(" ", scopes));
		}

		return Paseto.encrypt(sessionKey, builder.buildJson(), EMPTY_FOOTER);
	}

	/**
	 * Creates a token with full Strategiz claims structure for authentication flows.
	 * @param userId the internal user ID
	 * @param authenticationMethods list of authentication methods used
	 * @param acr authentication context reference ("0", "1", "2", "3")
	 * @param validity how long the token should be valid
	 * @param demoMode the user's demo mode (true for demo, false for live)
	 * @return the token string
	 */
	public String createAuthenticationToken(String userId, List<String> authenticationMethods, String acr,
			Duration validity, Boolean demoMode) {
		log.info("PasetoTokenIssuer.createAuthenticationToken - userId (sub claim): {}", userId);
		Instant now = Instant.now();
		Instant expiresAt = now.plus(validity);
		String tokenId = UUID.randomUUID().toString();

		List<Integer> amr = encodeAuthenticationMethods(authenticationMethods);
		String scope = calculateUserScopes(userId);

		String payload = PasetoClaimsUtil.builder()
			.subject(userId)
			.issuer(issuer)
			.audience(audience)
			.issuedAt(now)
			.expiration(expiresAt)
			.tokenId(tokenId)
			.claim("amr", amr)
			.claim("acr", acr)
			.claim("auth_time", now.getEpochSecond())
			.claim("scope", scope)
			.claim("type", "ACCESS")
			.claim("token_type", "session")
			.claim("demoMode", demoMode != null ? demoMode : true)
			.buildJson();

		return Paseto.encrypt(sessionKey, payload, EMPTY_FOOTER);
	}

	/**
	 * Creates a token with automatic ACR calculation based on authentication methods.
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
	 * Calculates Authentication Context Reference (ACR) based on authentication methods.
	 * ACR values: - "0": No authentication (partial/signup in progress) - "1":
	 * Single-factor authentication - "2": Multi-factor authentication (standard MFA) -
	 * "3": Strong multi-factor (hardware key + another factor)
	 * @param authenticationMethods list of authentication methods completed
	 * @param isPartialAuth whether this is partial authentication
	 * @return ACR value as string
	 */
	public String calculateAcr(List<String> authenticationMethods, boolean isPartialAuth) {
		if (isPartialAuth || authenticationMethods == null || authenticationMethods.isEmpty()) {
			return "0";
		}

		if (authenticationMethods.contains("passkeys") && authenticationMethods.size() > 1) {
			return "3";
		}

		if (authenticationMethods.size() >= 2 || authenticationMethods.contains("passkeys")) {
			return "2";
		}

		return "1";
	}

	/**
	 * Encodes authentication methods to numeric list for obfuscation.
	 * @param authMethods list of authentication method names
	 * @return list of numeric method identifiers
	 */
	public List<Integer> encodeAuthenticationMethods(List<String> authMethods) {
		if (authMethods == null || authMethods.isEmpty()) {
			return new ArrayList<>();
		}

		Map<String, Integer> methodMap = Map.ofEntries(Map.entry("password", 1), Map.entry("sms_otp", 2),
				Map.entry("passkeys", 3), Map.entry("totp", 4), Map.entry("email_otp", 5), Map.entry("backup_codes", 6),
				Map.entry("google", 7), Map.entry("facebook", 8), Map.entry("apple", 9), Map.entry("microsoft", 10));

		return authMethods.stream().filter(methodMap::containsKey).map(methodMap::get).collect(Collectors.toList());
	}

	/**
	 * Decodes numeric AMR back to authentication method names.
	 * @param amr list of numeric method identifiers
	 * @return list of authentication method names
	 */
	public List<String> decodeAuthenticationMethods(List<Integer> amr) {
		if (amr == null || amr.isEmpty()) {
			return new ArrayList<>();
		}

		Map<Integer, String> methodMap = Map.ofEntries(Map.entry(1, "password"), Map.entry(2, "sms_otp"),
				Map.entry(3, "passkeys"), Map.entry(4, "totp"), Map.entry(5, "email_otp"), Map.entry(6, "backup_codes"),
				Map.entry(7, "google"), Map.entry(8, "facebook"), Map.entry(9, "apple"), Map.entry(10, "microsoft"));

		return amr.stream().filter(methodMap::containsKey).map(methodMap::get).collect(Collectors.toList());
	}

	/**
	 * Returns the access token validity duration.
	 * @return Duration for access token validity
	 */
	public Duration getAccessTokenValidity() {
		return parseDuration(accessTokenValidity);
	}

	/**
	 * Returns the refresh token validity duration.
	 * @return Duration for refresh token validity
	 */
	public Duration getRefreshTokenValidity() {
		return parseDuration(refreshTokenValidity);
	}

	/**
	 * Calculates user scopes based on entitlements and permissions.
	 */
	private String calculateUserScopes(String userId) {
		List<String> scopes = Arrays.asList("read:profile", "write:profile", "read:portfolio", "write:portfolio",
				"read:positions", "write:positions", "read:market_data", "read:watchlists", "write:watchlists",
				"read:trades", "write:trades", "read:strategies", "write:strategies", "read:settings", "write:settings",
				"read:auth_methods", "write:auth_methods");

		return String.join(" ", scopes);
	}

	/**
	 * Parses a duration string like "30m", "1h", "7d".
	 */
	private Duration parseDuration(String duration) {
		if (duration == null || duration.isEmpty()) {
			return Duration.ofMinutes(30);
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
			return Duration.ofMinutes(30);
		}
	}

	/**
	 * Enum for token types
	 */
	public enum TokenType {

		ACCESS, REFRESH

	}

}
