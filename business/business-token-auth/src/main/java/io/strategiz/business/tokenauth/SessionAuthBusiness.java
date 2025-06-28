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

import java.time.Instant;
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
     * Creates and persists access and refresh tokens for a user
     *
     * @param userId User ID
     * @param deviceId Optional device ID
     * @param ipAddress IP address where the token is issued
     * @param scopes Optional scopes/roles
     * @return A TokenPair containing both tokens
     */
    public TokenPair createTokenPair(String userId, String deviceId, String ipAddress, String... scopes) {
        log.info("Creating token pair for user: {}", userId);
        
        // Create tokens
        String accessToken = tokenProvider.createAccessToken(userId, scopes);
        String refreshToken = tokenProvider.createRefreshToken(userId);
        
        // Extract claims
        Map<String, Object> accessClaims = tokenProvider.parseToken(accessToken);
        Map<String, Object> refreshClaims = tokenProvider.parseToken(refreshToken);
        
        // Store access token in database
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
                .claims(Map.of("scopes", String.join(" ", scopes)))
                .build();
        
        // Store refresh token in database
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
                .claims(Map.of())
                .build();
        
        tokenRepository.save(accessTokenEntity);
        tokenRepository.save(refreshTokenEntity);
        
        return new TokenPair(accessToken, refreshToken);
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
            
            // Create a new access token
            String newAccessToken = tokenProvider.createAccessToken(userId);
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
        // Create tokens without specific device ID since this is for session management
        TokenPair tokenPair = createTokenPair(userId, null, "internal", "session");
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
    
    /**
     * Generate an access token for a user
     * This is a convenience method that doesn't store the token in the repository
     *
     * @param userId User ID
     * @return The access token
     */
    public String generateToken(String userId) {
        log.debug("Generating access token for user: {}", userId);
        return tokenProvider.createAccessToken(userId);
    }
    
    /**
     * Generate a refresh token for a user
     * This is a convenience method that doesn't store the token in the repository
     *
     * @param userId User ID
     * @return The refresh token
     */
    public String generateRefreshToken(String userId) {
        log.debug("Generating refresh token for user: {}", userId);
        return tokenProvider.createRefreshToken(userId);
    }
}
