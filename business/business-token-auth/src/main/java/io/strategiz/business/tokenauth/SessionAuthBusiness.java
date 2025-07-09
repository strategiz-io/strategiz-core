package io.strategiz.business.tokenauth;

import dev.paseto.jpaseto.PasetoException;
import io.strategiz.data.auth.model.session.PasetoToken;
import io.strategiz.data.auth.repository.session.PasetoTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for session authentication and token management
 */
@Component
public class SessionAuthBusiness {
    private static final Logger log = LoggerFactory.getLogger(SessionAuthBusiness.class);
    
    @Value("${session.expiry.seconds:86400}") // Default to 24 hours
    private long sessionExpirySeconds;
    
    private final PasetoTokenProvider tokenProvider;
    private final PasetoTokenRepository tokenRepository;
    
    @Autowired
    public SessionAuthBusiness(PasetoTokenProvider tokenProvider, PasetoTokenRepository tokenRepository) {
        this.tokenProvider = tokenProvider;
        this.tokenRepository = tokenRepository;
    }
    

    
    /**
     * Creates authentication tokens with full Strategiz claims structure
     * 
     * This method implements the complete token specification with ACR/AAL separation.
     *
     * @param userId User ID
     * @param authenticationMethods List of authentication methods used
     * @param isPartialAuth Whether this is partial authentication (2FA mandatory but incomplete)
     * @param deviceId Optional device ID
     * @param ipAddress IP address where the token is issued
     * @return A TokenPair containing both tokens with full claims
     */
    public TokenPair createAuthenticationTokenPair(String userId, List<String> authenticationMethods, 
                                                  boolean isPartialAuth, String deviceId, String ipAddress) {
        String acr = tokenProvider.calculateAcr(authenticationMethods, isPartialAuth);
        String aal = tokenProvider.calculateAal(authenticationMethods);
        
        log.info("Creating authentication token pair for user: {} with ACR: {} AAL: {} and methods: {}", 
                userId, acr, aal, authenticationMethods);
        
        // Create access token with full authentication claims
        Duration accessValidity = Duration.ofHours(24); // 24 hours
        String accessToken = tokenProvider.createAuthenticationToken(userId, authenticationMethods, acr, aal, accessValidity);
        
        // Create refresh token (standard, no auth-specific claims needed)
        String refreshToken = tokenProvider.createRefreshToken(userId);
        
        // Extract claims for persistence
        Map<String, Object> accessClaims = tokenProvider.parseToken(accessToken);
        Map<String, Object> refreshClaims = tokenProvider.parseToken(refreshToken);
        
        // Store access token with authentication metadata
        Map<String, Object> accessTokenMetadata = new HashMap<>();
        accessTokenMetadata.put("amr", accessClaims.get("amr"));
        accessTokenMetadata.put("acr", acr);
        accessTokenMetadata.put("aal", aal);
        accessTokenMetadata.put("auth_methods", String.join(",", authenticationMethods));
        accessTokenMetadata.put("scope", accessClaims.get("scope"));
        
        PasetoToken accessTokenEntity = PasetoToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .tokenType("ACCESS")
                .tokenValue(accessToken)
                .issuedAt(getClaimAsLong(accessClaims, "iat"))
                .expiresAt(getClaimAsLong(accessClaims, "exp"))
                .deviceId(deviceId)
                .issuedFrom(ipAddress)
                .revoked(false)
                .claims(accessTokenMetadata)
                .build();
        
        // Store refresh token
        PasetoToken refreshTokenEntity = PasetoToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .tokenType("REFRESH")
                .tokenValue(refreshToken)
                .issuedAt(getClaimAsLong(refreshClaims, "iat"))
                .expiresAt(getClaimAsLong(refreshClaims, "exp"))
                .deviceId(deviceId)
                .issuedFrom(ipAddress)
                .revoked(false)
                .claims(Map.of("associated_access_token", accessTokenEntity.getId()))
                .build();
        
        tokenRepository.save(accessTokenEntity);
        tokenRepository.save(refreshTokenEntity);
        
        log.info("Successfully created authentication tokens for user: {} with {} methods", 
                userId, authenticationMethods.size());
        
        return new TokenPair(accessToken, refreshToken);
    }
    
    /**
     * Updates authentication tokens when new authenticator is completed during session
     * This handles step-up authentication scenarios
     *
     * @param currentAccessToken The current access token
     * @param additionalAuthMethods New authentication methods completed
     * @param deviceId Optional device ID
     * @param ipAddress IP address where the update is requested
     * @return A new TokenPair with updated ACR/AAL claims
     * @throws PasetoException if current token is invalid
     */
    public TokenPair updateAuthenticationTokenPair(String currentAccessToken, List<String> additionalAuthMethods,
                                                  String deviceId, String ipAddress) throws PasetoException {
        log.info("Updating authentication tokens with additional methods: {}", additionalAuthMethods);
        
        // Parse current token to get user info and existing auth methods
        Map<String, Object> currentClaims = tokenProvider.parseToken(currentAccessToken);
        String userId = (String) currentClaims.get("sub");
        
        // Get current authentication methods from AMR
        int[] currentAmr = (int[]) currentClaims.get("amr");
        List<String> currentMethods = decodeAuthenticationMethods(currentAmr);
        
        // Combine current and additional methods
        List<String> combinedMethods = new ArrayList<>(currentMethods);
        combinedMethods.addAll(additionalAuthMethods);
        
        // Create new token pair with updated authentication - assume full auth now
        TokenPair newTokens = createAuthenticationTokenPair(userId, combinedMethods, false, deviceId, ipAddress);
        
        // Revoke the old access token
        revokeToken(currentAccessToken);
        
        log.info("Successfully updated authentication tokens for user: {} with combined methods: {}", 
                userId, combinedMethods);
        
        return newTokens;
    }
    
    /**
     * Decodes numeric AMR back to authentication method names (helper method)
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
     * Legacy compatibility method - maintains backward compatibility with old ACR format
     * 
     * @param userId User ID
     * @param authenticationMethods List of authentication methods used
     * @param acr Authentication Context Class (legacy format like "2.1", "2.2", "2.3")
     * @param deviceId Optional device ID
     * @param ipAddress IP address where the token is issued
     * @return A TokenPair containing both tokens with full claims
     * @deprecated Use createAuthenticationTokenPair(userId, authMethods, isPartialAuth, deviceId, ipAddress) instead
     */
    @Deprecated
    public TokenPair createAuthenticationTokenPair(String userId, List<String> authenticationMethods, 
                                                  String acr, String deviceId, String ipAddress) {
        // Convert legacy ACR to new format
        boolean isPartialAuth = "1".equals(acr);
        return createAuthenticationTokenPair(userId, authenticationMethods, isPartialAuth, deviceId, ipAddress);
    }
    
    /**
     * Helper method to handle different claim value types (Number or String) and convert to long
     * 
     * @param claims The claims map
     * @param key The key of the claim
     * @return The claim value as a long
     */
    private long getClaimAsLong(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            String strValue = (String) value;
            try {
                // First try to parse as a simple number
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                try {
                    // Try to parse as ISO 8601 format if direct parsing fails
                    log.debug("Parsing ISO timestamp for claim {}: {}", key, strValue);
                    return Instant.parse(strValue).getEpochSecond();
                } catch (Exception dateException) {
                    log.warn("Failed to parse claim {} ({}): {}", key, strValue, dateException.getMessage());
                    return Instant.now().getEpochSecond();
                }
            }
        } else {
            log.warn("Unexpected claim type for {}: {}", key, value != null ? value.getClass() : "null");
            return Instant.now().getEpochSecond(); // Fallback to current time if claim is missing or of unexpected type
        }
    }
    
    /**
     * Refreshes an access token using a valid refresh token
     *
     * @param refreshToken The refresh token
     * @param ipAddress IP address where the refresh is requested
     * @return A new access token wrapped in Optional, or empty if refresh token is invalid
     */
    public Optional<String> refreshAccessToken(String refreshToken, String ipAddress) {
        try {
            if (!tokenProvider.isValidRefreshToken(refreshToken)) {
                log.warn("Invalid refresh token format");
                return Optional.empty();
            }
            
            // Verify the token exists in the database and is not revoked
            Optional<PasetoToken> storedToken = tokenRepository.findByTokenValue(refreshToken);
            if (storedToken.isEmpty() || !storedToken.get().isValid()) {
                log.warn("Refresh token not found or invalid");
                return Optional.empty();
            }
            
            PasetoToken refreshTokenEntity = storedToken.get();
            String userId = refreshTokenEntity.getUserId();
            
            // Create a new access token using stored authentication metadata
            // Try to preserve the original authentication method and ACR level
            Map<String, Object> originalClaims = refreshTokenEntity.getClaims();
            String originalAcr = (String) originalClaims.getOrDefault("acr", "2.1");
            String authMethodsStr = (String) originalClaims.getOrDefault("auth_methods", "password");
            List<String> authMethods = List.of(authMethodsStr.split(","));
            
            // Calculate AAL based on authentication methods
            String originalAal = tokenProvider.calculateAal(authMethods);
            
            Duration accessValidity = Duration.ofHours(24); // 24 hours
            String newAccessToken = tokenProvider.createAuthenticationToken(userId, authMethods, originalAcr, originalAal, accessValidity);
            Map<String, Object> accessClaims = tokenProvider.parseToken(newAccessToken);
            
            // Store the new access token
            PasetoToken accessTokenEntity = PasetoToken.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .tokenType("ACCESS")
                    .tokenValue(newAccessToken)
                    .issuedAt(((Number) accessClaims.get("iat")).longValue())
                    .expiresAt(((Number) accessClaims.get("exp")).longValue())
                    .deviceId(refreshTokenEntity.getDeviceId())
                    .issuedFrom(ipAddress)
                    .revoked(false)
                    .build();
            
            tokenRepository.save(accessTokenEntity);
            log.info("Refreshed access token for user: {}", userId);
            
            return Optional.of(newAccessToken);
        } catch (PasetoException e) {
            log.error("Error refreshing access token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Validates an access token
     *
     * @param token The token to validate
     * @return The user ID if valid, empty if invalid
     */
    public Optional<String> validateAccessToken(String token) {
        try {
            if (!tokenProvider.isValidAccessToken(token)) {
                return Optional.empty();
            }
            
            Optional<PasetoToken> storedToken = tokenRepository.findByTokenValue(token);
            if (storedToken.isEmpty() || !storedToken.get().isValid()) {
                return Optional.empty();
            }
            
            return Optional.of(storedToken.get().getUserId());
        } catch (Exception e) {
            log.error("Error validating access token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Revokes a specific token
     *
     * @param token The token to revoke
     * @return true if revoked, false otherwise
     */
    public boolean revokeToken(String token) {
        try {
            Optional<PasetoToken> storedToken = tokenRepository.findByTokenValue(token);
            if (storedToken.isEmpty()) {
                return false;
            }
            
            PasetoToken tokenEntity = storedToken.get();
            tokenEntity.setRevoked(true);
            tokenEntity.setRevokedAt(Instant.now().getEpochSecond());
            tokenEntity.setRevocationReason("Manually revoked");
            
            tokenRepository.save(tokenEntity);
            log.info("Revoked token: {}", tokenEntity.getId());
            
            return true;
        } catch (Exception e) {
            log.error("Error revoking token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Revokes all tokens for a user
     *
     * @param userId User ID
     * @return Number of tokens revoked
     */
    public int revokeAllUserTokens(String userId) {
        List<PasetoToken> tokens = tokenRepository.findAllByUserId(userId);
        
        int count = 0;
        for (PasetoToken token : tokens) {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now().getEpochSecond());
                token.setRevocationReason("All user tokens revoked");
                
                tokenRepository.save(token);
                count++;
            }
        }
        
        log.info("Revoked {} tokens for user: {}", count, userId);
        return count;
    }
    
    /**
     * Gets all active tokens for a user
     *
     * @param userId User ID
     * @return List of active tokens
     */
    public List<PasetoToken> getActiveUserTokens(String userId) {
        return tokenRepository.findActiveTokensByUserId(userId);
    }
    
    /**
     * Create a session for a user using PASETO tokens
     * This replaces the old SessionService createSession functionality
     *
     * @param userId User ID
     * @return The access token for the session
     */
    public String createSession(String userId) {
        log.info("Creating session for user: {}", userId);
        // Create tokens with basic authentication for session management
        // Using false for isPartialAuth since sessions are for completed authentication
        TokenPair tokenPair = createAuthenticationTokenPair(
            userId,
            List.of("password"), // Default auth method for internal sessions
            false, // Not partial auth - full session
            null, // No device ID for sessions
            "internal" // Internal session creation
        );
        return tokenPair.accessToken();
    }
    
    /**
     * Validate a session token
     * This replaces the old SessionService validateSession functionality
     *
     * @param token Session token
     * @return Optional containing the user ID if valid, empty otherwise
     */
    public Optional<String> validateSession(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Session token is null or empty");
            return Optional.empty();
        }
        
        log.debug("Validating session token");
        return validateAccessToken(token);
    }
    
    /**
     * Delete a session token
     * This replaces the old SessionService deleteSession functionality
     *
     * @param token Session token
     * @return true if deleted, false otherwise
     */
    public boolean deleteSession(String token) {
        log.info("Deleting session token");
        return revokeToken(token);
    }
    
    /**
     * Delete all sessions for a user
     * This replaces the old SessionService deleteUserSessions functionality
     *
     * @param userId User ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteUserSessions(String userId) {
        log.info("Deleting all sessions for user: {}", userId);
        int count = revokeAllUserTokens(userId);
        return count > 0;
    }
    
    /**
     * Scheduled task to clean up expired tokens
     * Runs every hour
     * This replaces the old SessionService cleanupExpiredSessions functionality
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired tokens");
        long now = Instant.now().getEpochSecond();
        int count = tokenRepository.deleteExpiredTokens(now);
        log.info("Deleted {} expired tokens", count);
    }
    
    /**
     * Simple container for access and refresh token pair
     */
    public record TokenPair(String accessToken, String refreshToken) {}
    

}
