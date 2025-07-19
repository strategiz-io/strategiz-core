package io.strategiz.business.tokenauth;

import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.Pasetos;
import dev.paseto.jpaseto.PasetoException;
import dev.paseto.jpaseto.lang.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
        
        // Build the token using the PASETO v2 local builder
        var builder = Pasetos.V2.LOCAL.builder()
                .setSharedSecret(secretKey)
                .setExpiration(expiresAt)
                .setIssuedAt(now)
                .setIssuer(issuer)
                .setAudience(audience)
                .setSubject(generatePublicUserId(userId))
                .setKeyId(tokenId);
                
        // Add the token type
        builder.claim("type", tokenType.name());
                
        // Add scopes if present
        if (scopes != null && scopes.length > 0) {
            builder.claim("scope", String.join(" ", scopes));
        }
                
        return builder.compact();
    }
    
    /**
     * Creates a token with full Strategiz claims structure for authentication flows
     *
     * @param userId the internal user ID
     * @param authenticationMethods list of authentication methods used
     * @param acr authentication context class ("0", "1", "2")
     * @param aal authenticator assurance level ("1", "2", "3")
     * @param validity how long the token should be valid
     * @return the token string
     */
    public String createAuthenticationToken(String userId, List<String> authenticationMethods, 
                                          String acr, String aal, Duration validity) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(validity);
        String tokenId = UUID.randomUUID().toString();
        String publicUserId = generatePublicUserId(userId);
        
        // Convert auth methods to numeric AMR
        int[] amr = encodeAuthenticationMethods(authenticationMethods);
        
        // Calculate scopes based on ACR and AAL
        String scope = calculateScopesForAcr(acr, aal, userId);
        
        // Build the token with full claims structure
        var builder = Pasetos.V2.LOCAL.builder()
                .setSharedSecret(secretKey)
                .setExpiration(expiresAt)
                .setIssuedAt(now)
                .setIssuer(issuer)
                .setAudience(audience)
                .setSubject(publicUserId)
                .setKeyId(tokenId);
                
        // Add Strategiz-specific claims
        builder.claim("amr", amr);              // Authentication Methods (numeric)
        builder.claim("acr", acr);              // Authentication Context Class ("0", "1", "2")
        builder.claim("aal", aal);              // Authenticator Assurance Level ("1", "2", "3")
        builder.claim("auth_time", now.getEpochSecond()); // When user authenticated
        builder.claim("scope", scope);          // Calculated permissions
        builder.claim("type", "ACCESS");        // Token type
                
        return builder.compact();
    }
    
    /**
     * Creates a token with automatic ACR/AAL calculation based on authentication methods
     *
     * @param userId the internal user ID
     * @param authenticationMethods list of authentication methods used
     * @param isPartialAuth whether this is partial authentication (2FA mandatory but incomplete)
     * @param validity how long the token should be valid
     * @return the token string
     */
    public String createAuthenticationToken(String userId, List<String> authenticationMethods, 
                                          boolean isPartialAuth, Duration validity) {
        String acr = calculateAcr(authenticationMethods, isPartialAuth);
        String aal = calculateAal(authenticationMethods);
        return createAuthenticationToken(userId, authenticationMethods, acr, aal, validity);
    }
    
    /**
     * Updates token claims when new authenticator is added during session
     * This creates a new token with updated ACR/AAL based on additional authentication
     *
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
        
        // Get current authentication methods from AMR - handle both ArrayList and int[] cases
        Object amrClaim = currentClaims.get("amr");
        int[] currentAmr = null;
        
        if (amrClaim instanceof int[]) {
            currentAmr = (int[]) amrClaim;
        } else if (amrClaim instanceof List<?>) {
            // Handle ArrayList case - convert to int[]
            List<?> amrList = (List<?>) amrClaim;
            currentAmr = new int[amrList.size()];
            for (int i = 0; i < amrList.size(); i++) {
                Object value = amrList.get(i);
                if (value instanceof Number) {
                    currentAmr[i] = ((Number) value).intValue();
                }
            }
        }
        
        List<String> currentMethods = decodeAuthenticationMethods(currentAmr);
        
        // Combine current and additional methods
        List<String> combinedMethods = new ArrayList<>(currentMethods);
        combinedMethods.addAll(additionalAuthMethods);
        
        // Calculate new ACR/AAL - assume full authentication now
        String newAcr = calculateAcr(combinedMethods, false);
        String newAal = calculateAal(combinedMethods);
        
        return createAuthenticationToken(userId, combinedMethods, newAcr, newAal, validity);
    }
    
    /**
     * Decodes numeric AMR back to authentication method names
     *
     * @param amr array of numeric method identifiers
     * @return list of authentication method names
     */
    private List<String> decodeAuthenticationMethods(int[] amr) {
        if (amr == null || amr.length == 0) {
            return new ArrayList<>();
        }
        
        // Reverse mapping of authentication methods
        Map<Integer, String> methodMap = Map.of(
            1, "password",
            2, "sms_otp", 
            3, "passkeys",
            4, "totp",
            5, "email_otp",
            6, "backup_codes"
        );
        
        List<String> methods = new ArrayList<>();
        for (int methodId : amr) {
            if (methodMap.containsKey(methodId)) {
                methods.add(methodMap.get(methodId));
            }
        }
        
        return methods;
    }
    
    /**
     * Generates a public user ID from internal user ID
     * Format: usr_pub_{16_chars}
     *
     * @param internalUserId the internal user ID
     * @return public user ID safe for exposure
     */
    private String generatePublicUserId(String internalUserId) {
        // For now, use a simple hash-based approach
        // In production, this should use a proper encoding/mapping system
        String hash = Integer.toHexString(internalUserId.hashCode());
        return "usr_pub_" + hash.substring(0, Math.min(hash.length(), 16));
    }
    
    /**
     * Encodes authentication methods to numeric array for obfuscation
     *
     * @param authMethods list of authentication method names
     * @return array of numeric method identifiers
     */
    private int[] encodeAuthenticationMethods(List<String> authMethods) {
        if (authMethods == null || authMethods.isEmpty()) {
            return new int[0];
        }
        
        // Authentication method mapping
        Map<String, Integer> methodMap = Map.of(
            "password", 1,
            "sms_otp", 2,
            "passkeys", 3,
            "totp", 4,
            "email_otp", 5,
            "backup_codes", 6
        );
        
        return authMethods.stream()
                .filter(methodMap::containsKey)
                .mapToInt(methodMap::get)
                .toArray();
    }
    
    /**
     * Calculates Authentication Context Reference (ACR) based on authentication state
     * 
     * @param authenticationMethods list of authentication methods completed
     * @param isPartialAuth whether this is partial authentication (2FA mandatory but incomplete)
     * @return ACR value ("0", "1", "2")
     */
    public String calculateAcr(List<String> authenticationMethods, boolean isPartialAuth) {
        if (authenticationMethods == null || authenticationMethods.isEmpty()) {
            return "0"; // Unauthenticated
        }
        
        if (isPartialAuth) {
            return "1"; // Partially authenticated (2FA mandatory but only 1 factor completed)
        }
        
        return "2"; // Fully authenticated
    }
    
    /**
     * Calculates Authenticator Assurance Level (AAL) based on authentication methods
     * 
     * @param authenticationMethods list of authentication methods completed
     * @return AAL value ("1", "2", "3")
     */
    public String calculateAal(List<String> authenticationMethods) {
        if (authenticationMethods == null || authenticationMethods.isEmpty()) {
            return "1"; // Default to basic assurance
        }
        
        // AAL 3 - Hardware crypto (passkeys)
        if (authenticationMethods.contains("passkeys")) {
            return "3";
        }
        
        // AAL 2 - Multi-factor (password + something else)
        if (authenticationMethods.size() > 1 && 
            authenticationMethods.contains("password") && 
            (authenticationMethods.contains("totp") || authenticationMethods.contains("sms_otp"))) {
            return "2";
        }
        
        // AAL 1 - Single factor
        return "1";
    }
    
    /**
     * Calculates scopes based on Authentication Context Class (ACR) and Authenticator Assurance Level (AAL)
     *
     * @param acr the authentication context class ("0", "1", "2")
     * @param aal the authenticator assurance level ("1", "2", "3")
     * @param userId the user ID (for future user-specific permissions)
     * @return space-separated scope string
     */
    private String calculateScopesForAcr(String acr, String aal, String userId) {
        List<String> scopes = new ArrayList<>();
        
        switch (acr) {
            case "0":
                // No authentication - no scopes
                break;
                
            case "1":
                // Partial authentication - profile and auth method management only
                scopes.addAll(Arrays.asList(
                    "read:profile", "write:profile",
                    "read:auth_methods", "write:auth_methods"
                ));
                break;
                
            case "2":
                // Fully authenticated - permissions based on AAL
                scopes.addAll(Arrays.asList(
                    "read:profile", "write:profile",
                    "read:portfolio", "read:positions", "read:market_data",
                    "read:watchlists", "write:watchlists"
                ));
                
                // AAL 2+ gets trading operations
                if ("2".equals(aal) || "3".equals(aal)) {
                    scopes.addAll(Arrays.asList(
                        "write:portfolio", "write:positions",
                        "read:trades", "write:trades",
                        "read:strategies", "write:strategies",
                        "read:settings", "write:settings"
                    ));
                }
                
                // AAL 3 gets sensitive operations
                if ("3".equals(aal)) {
                    scopes.addAll(Arrays.asList(
                        "delete:profile", "admin:portfolio",
                        "delete:positions", "delete:trades",
                        "admin:strategies", "admin:settings",
                        "read:auth_methods", "write:auth_methods",
                        "read:sessions", "write:sessions"
                    ));
                }
                break;
                
            default:
                // Unknown ACR - minimal scopes
                scopes.addAll(Arrays.asList("read:profile"));
                break;
        }
        
        return String.join(" ", scopes);
    }
    
    /**
     * Legacy method for backward compatibility - calculates ACR/AAL internally
     */
    private String calculateScopesForAcr(String acr, String userId) {
        // Extract AAL from legacy ACR format if needed
        String aal = "1"; // Default
        if (acr.contains(".")) {
            String[] parts = acr.split("\\.");
            if (parts.length == 2) {
                aal = parts[1];
                acr = parts[0];
            }
        }
        
        return calculateScopesForAcr(acr, aal, userId);
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
