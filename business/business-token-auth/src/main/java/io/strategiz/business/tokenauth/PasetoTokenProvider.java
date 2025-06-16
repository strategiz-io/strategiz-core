package io.strategiz.business.tokenauth;

import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.Pasetos;
import dev.paseto.jpaseto.PasetoException;
import dev.paseto.jpaseto.lang.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provides PASETO token generation and validation functionality.
 * This utility supports both V2 (local/symmetric) and V4 (public/asymmetric) tokens.
 */
@Component
public class PasetoTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(PasetoTokenProvider.class);
    
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
     * Whether to use V4 (public/asymmetric) or V2 (local/symmetric)
     */
    @Value("${auth.token.version:v2}")
    private String tokenVersion;

    /**
     * Secret key for V2 tokens (base64 encoded)
     */
    @Value("${auth.token.secret:}")
    private String secret;
    
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

    private SecretKey secretKey; // For V2 tokens
    
    /**
     * Initialize the token provider with the appropriate keys
     */
    @PostConstruct
    public void init() {
        log.info("Initializing PASETO V2 token provider");
        if (secret != null && !secret.isEmpty()) {
            secretKey = Keys.secretKey(Base64.getDecoder().decode(secret));
        } else {
            // Generate a random key for development
            SecureRandom secureRandom = new SecureRandom();
            byte[] key = new byte[32]; // 256 bits key
            secureRandom.nextBytes(key);
            secretKey = Keys.secretKey(key);
            log.warn("Using randomly generated secret key. In production, provide a stable key.");
        }
    }

    /**
     * Creates a new access token for a user
     *
     * @param userId the user ID
     * @param scopes optional scopes/roles
     * @return the token string
     */
    public String createAccessToken(String userId, String... scopes) {
        return createToken(userId, parseDuration(accessTokenValidity), TokenType.ACCESS, scopes);
    }
    
    /**
     * Creates a new refresh token for a user
     *
     * @param userId the user ID
     * @return the token string
     */
    public String createRefreshToken(String userId) {
        return createToken(userId, parseDuration(refreshTokenValidity), TokenType.REFRESH);
    }

    /**
     * Creates a new token with specified parameters
     *
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
        
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", userId);
        claimsMap.put("jti", tokenId);
        claimsMap.put("iat", now.getEpochSecond());
        claimsMap.put("exp", expiresAt.getEpochSecond());
        claimsMap.put("iss", issuer);
        claimsMap.put("aud", audience);
        claimsMap.put("type", tokenType.name());
        
        if (scopes != null && scopes.length > 0) {
            claimsMap.put("scope", String.join(" ", scopes));
        }
        
        return Pasetos.V2.LOCAL.builder()
                .setSharedSecret(secretKey)
                .claim("sub", claimsMap.get("sub"))
                .claim("jti", claimsMap.get("jti"))
                .claim("iat", claimsMap.get("iat"))
                .claim("exp", claimsMap.get("exp"))
                .claim("iss", claimsMap.get("iss"))
                .claim("aud", claimsMap.get("aud"))
                .claim("type", claimsMap.get("type"))
                .claim("scope", claimsMap.get("scope"))
                .compact();
    }
    
    /**
     * Parses and validates a token
     *
     * @param token the token to validate
     * @return the claims from the token
     * @throws PasetoException if the token is invalid or expired
     */
    public Map<String, Object> parseToken(String token) throws PasetoException {
        Paseto paseto = Pasetos.parserBuilder()
                .setSharedSecret(secretKey)
                .build()
                .parse(token);
        return paseto.getClaims();
    }
    
    /**
     * Gets the user ID from a token
     *
     * @param token the token
     * @return the user ID
     * @throws PasetoException if the token is invalid
     */
    public String getUserIdFromToken(String token) throws PasetoException {
        return (String) parseToken(token).get("sub");
    }
    
    /**
     * Validates if a token is an access token
     * 
     * @param token the token to validate
     * @return true if valid
     */
    public boolean isValidAccessToken(String token) {
        try {
            Map<String, Object> claims = parseToken(token);
            String tokenType = (String) claims.get("type");
            return TokenType.ACCESS.name().equals(tokenType);
        } catch (PasetoException e) {
            log.error("Invalid token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates if a token is a refresh token
     * 
     * @param token the token to validate
     * @return true if valid
     */
    public boolean isValidRefreshToken(String token) {
        try {
            Map<String, Object> claims = parseToken(token);
            String tokenType = (String) claims.get("type");
            return TokenType.REFRESH.name().equals(tokenType);
        } catch (PasetoException e) {
            log.error("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parses a duration string like "30m", "1h", "7d"
     * 
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
        } catch (NumberFormatException e) {
            log.error("Invalid duration format: {}", duration);
            return Duration.ofMinutes(30); // Default to 30 minutes
        }
    }

    /**
     * Enum for token types
     */
    public enum TokenType {
        ACCESS,
        REFRESH
    }
}
