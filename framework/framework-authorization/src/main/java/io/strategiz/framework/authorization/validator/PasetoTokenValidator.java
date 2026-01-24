package io.strategiz.framework.authorization.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.secrets.service.VaultSecretService;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * PASETO Token Validator - Single source of truth for ALL token validation. This is the
 * ONLY class that should validate PASETO tokens in the system.
 *
 * <p>Validation includes:
 *
 * <ul>
 *   <li>PASETO v4.local decryption and verification
 *   <li>Token expiration checking
 *   <li>Token type verification
 *   <li>Claims extraction
 * </ul>
 *
 * <p>This class does NOT perform database lookups. Token validation is purely
 * cryptographic. Session revocation checking is a separate concern handled by the
 * business layer if needed.
 *
 * <p>Uses paseto4j library for PASETO v4.local token validation.
 */
@Component
@DependsOn("vaultSecretService")
public class PasetoTokenValidator {

  private static final Logger log = LoggerFactory.getLogger(PasetoTokenValidator.class);

  private static final String EMPTY_FOOTER = "";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};

  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  @Autowired(required = false)
  private VaultSecretService vaultSecretService;

  private SecretKey identityKey;

  private SecretKey sessionKey;

  private boolean keysLoaded = false;

  /** Initialize the validator with keys from Vault. */
  @PostConstruct
  public void init() {
    log.info("Initializing PasetoTokenValidator with dual-key system (v4.local)");

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
      } catch (Exception e) {
        log.error("Failed to load keys from Vault: {} - {}",
            e.getClass().getSimpleName(), e.getMessage());
      }
    } else {
      log.error("VaultSecretService is null - cannot load token keys");
    }

    if (identityKey == null || sessionKey == null) {
      log.error("CRITICAL: Token keys not found in Vault. "
          + "Token validation will fail until keys are loaded.");
      log.error("Check Vault connectivity and ensure keys exist at: "
          + "tokens.{}.identity-key and tokens.{}.session-key", env, env);
      keysLoaded = false;
    } else {
      keysLoaded = true;
      log.info("PasetoTokenValidator initialized successfully with keys for environment: {}",
          env);
    }
  }

  /** Check if the validator has loaded keys and is operational. */
  public boolean isOperational() {
    return keysLoaded && identityKey != null && sessionKey != null;
  }

  /**
   * Validates a token and returns the claims if valid. This method performs ONLY
   * cryptographic validation - no database lookups.
   *
   * @param token the token to validate
   * @return the claims from the token
   * @throws PasetoException if the token is invalid, expired, or decryption fails
   */
  public Map<String, Object> parseToken(String token) throws PasetoException {
    // Check if keys are loaded
    if (!keysLoaded) {
      log.warn("Token validation attempted but keys not loaded");
      throw new PasetoException("Token validation unavailable - keys not loaded");
    }

    // Try session key first (most common case)
    try {
      return parseWithKey(token, sessionKey);
    } catch (PasetoException e) {
      // If session key fails, try identity key
      try {
        Map<String, Object> claims = parseWithKey(token, identityKey);
        // Verify it's actually an identity token
        String tokenType = (String) claims.get("token_type");
        if ("identity".equals(tokenType) || "recovery".equals(tokenType)) {
          return claims;
        }
        throw new PasetoException("Token validation failed - not an identity or recovery token");
      } catch (PasetoException identityException) {
        // Both keys failed, throw original exception
        throw e;
      }
    }
  }

  /**
   * Validates a token and extracts it into an AuthenticatedUser object.
   *
   * @param token the token to validate
   * @return Optional containing the AuthenticatedUser if valid, empty if invalid
   */
  public Optional<AuthenticatedUser> validateAndExtract(String token) {
    try {
      Map<String, Object> claims = parseToken(token);
      return Optional.of(extractAuthenticatedUser(claims));
    } catch (PasetoException e) {
      log.debug("Token validation failed: {}", e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Gets the user ID from a token.
   *
   * @param token the token
   * @return the user ID
   * @throws PasetoException if the token is invalid
   */
  public String getUserIdFromToken(String token) throws PasetoException {
    return (String) parseToken(token).get("sub");
  }

  /**
   * Validates if a token is a valid access/session token.
   *
   * @param token the token to validate
   * @return true if valid
   */
  public boolean isValidAccessToken(String token) {
    try {
      Map<String, Object> claims = parseToken(token);
      String tokenType = (String) claims.get("token_type");
      log.debug("Token claims - token_type: {}, type: {}", tokenType, claims.get("type"));
      return "session".equals(tokenType) || "ACCESS".equals(claims.get("type"));
    } catch (PasetoException e) {
      log.debug("Invalid access token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Validates if a token is a valid identity token.
   *
   * @param token the token to validate
   * @return true if valid identity token
   */
  public boolean isValidIdentityToken(String token) {
    try {
      Map<String, Object> claims = parseToken(token);
      String tokenType = (String) claims.get("token_type");
      return "identity".equals(tokenType);
    } catch (PasetoException e) {
      log.debug("Invalid identity token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Validates if a token is a valid refresh token.
   *
   * @param token the token to validate
   * @return true if valid
   */
  public boolean isValidRefreshToken(String token) {
    try {
      Map<String, Object> claims = parseToken(token);
      String type = (String) claims.get("type");
      return "REFRESH".equals(type);
    } catch (PasetoException e) {
      log.debug("Invalid refresh token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Validates if a token is a valid recovery token.
   *
   * @param token the token to validate
   * @return true if valid recovery token
   */
  public boolean isValidRecoveryToken(String token) {
    try {
      Map<String, Object> claims = parseToken(token);
      String tokenType = (String) claims.get("token_type");
      return "recovery".equals(tokenType);
    } catch (PasetoException e) {
      log.debug("Invalid recovery token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Validates a recovery token and returns the recovery ID if valid.
   *
   * @param token the token to validate
   * @return Optional containing the recovery ID if valid, empty if invalid
   */
  public Optional<String> validateRecoveryToken(String token) {
    try {
      Map<String, Object> claims = parseToken(token);
      String tokenType = (String) claims.get("token_type");
      if (!"recovery".equals(tokenType)) {
        log.debug("Token is not a recovery token");
        return Optional.empty();
      }
      String recoveryId = (String) claims.get("recovery_id");
      return Optional.ofNullable(recoveryId);
    } catch (PasetoException e) {
      log.debug("Invalid recovery token: {}", e.getMessage());
      return Optional.empty();
    }
  }

  /** Extracts claims and creates an AuthenticatedUser object. */
  private AuthenticatedUser extractAuthenticatedUser(Map<String, Object> claims) {
    String userId = (String) claims.get("sub");
    String acr = (String) claims.getOrDefault("acr", "0");
    Boolean demoMode = (Boolean) claims.getOrDefault("demoMode", true);

    Set<String> scopes = parseScopes((String) claims.get("scope"));
    List<Integer> amr = parseAmr(claims.get("amr"));
    Instant authTime = parseInstant(claims.get("iat"));
    Instant tokenExpiry = parseInstant(claims.get("exp"));

    return AuthenticatedUser.builder()
        .userId(userId)
        .scopes(scopes)
        .acr(acr)
        .amr(amr)
        .demoMode(demoMode)
        .authTime(authTime)
        .tokenExpiry(tokenExpiry)
        .build();
  }

  /** Parse token with a specific key and validate expiration. */
  private Map<String, Object> parseWithKey(String token, SecretKey key) throws PasetoException {
    try {
      // Decrypt the token
      String payload = Paseto.decrypt(key, token, EMPTY_FOOTER);

      // Parse JSON payload to claims map
      Map<String, Object> claims = objectMapper.readValue(payload, MAP_TYPE_REF);

      // Validate expiration (jpaseto did this automatically, now we do it manually)
      validateExpiration(claims);

      return claims;
    } catch (JsonProcessingException e) {
      throw new PasetoException("Failed to parse token claims: " + e.getMessage());
    }
  }

  /** Validates token expiration. Throws exception if token is expired. */
  private void validateExpiration(Map<String, Object> claims) throws PasetoException {
    Object expObj = claims.get("exp");
    if (expObj == null) {
      throw new PasetoException("Token has no expiration claim");
    }

    Instant exp = parseInstant(expObj);
    if (exp == null) {
      throw new PasetoException("Invalid expiration claim format");
    }

    if (Instant.now().isAfter(exp)) {
      throw new PasetoException("Token has expired");
    }
  }

  private Set<String> parseScopes(String scopeString) {
    if (scopeString == null || scopeString.isBlank()) {
      return Set.of();
    }
    return new HashSet<>(Arrays.asList(scopeString.split("\\s+")));
  }

  @SuppressWarnings("unchecked")
  private List<Integer> parseAmr(Object amrObj) {
    if (amrObj == null) {
      return List.of();
    }
    if (amrObj instanceof List<?>) {
      List<Integer> result = new ArrayList<>();
      for (Object item : (List<?>) amrObj) {
        if (item instanceof Integer) {
          result.add((Integer) item);
        } else if (item instanceof Number) {
          result.add(((Number) item).intValue());
        }
      }
      return result;
    }
    return List.of();
  }

  private Instant parseInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant) {
      return (Instant) value;
    }
    if (value instanceof Long) {
      return Instant.ofEpochSecond((Long) value);
    }
    if (value instanceof Number) {
      return Instant.ofEpochSecond(((Number) value).longValue());
    }
    return null;
  }

  /**
   * Returns the session key for internal use (e.g., by the filter). Package-private to
   * restrict access.
   */
  SecretKey getSessionKey() {
    return sessionKey;
  }

  /**
   * Returns the identity key for internal use (e.g., by the filter). Package-private to
   * restrict access.
   */
  SecretKey getIdentityKey() {
    return identityKey;
  }

}
