package io.strategiz.business.tokenauth;

import dev.paseto.jpaseto.PasetoException;
import io.strategiz.business.tokenauth.model.SessionValidationResult;
import io.strategiz.data.auth.entity.session.PasetoTokenEntity;
import io.strategiz.data.auth.repository.session.PasetoTokenRepository;
import io.strategiz.data.session.entity.UserSession;
import io.strategiz.data.session.entity.UserSessionEntity;
import io.strategiz.data.session.repository.SessionRepository;
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
 * Business logic for token and session management
 * 
 * Architecture Responsibilities:
 * - PASETO token generation and validation (business logic)
 * - Session creation delegation to data-session module (orchestration)
 * - Authentication flow coordination
 * 
 * Flow:
 * Service Layer → Business Layer (this) → Data Layer (data-session)
 * 
 * This class:
 * ✅ Creates PASETO tokens (business logic)
 * ✅ Orchestrates authentication flow  
 * ✅ Delegates session persistence to data-session module
 * ❌ NO servlet dependencies - pure business logic
 * ❌ NO direct HTTP handling - service layer responsibility
 */
@Component
public class SessionAuthBusiness {
    private static final Logger log = LoggerFactory.getLogger(SessionAuthBusiness.class);
    
    @Value("${session.expiry.seconds:86400}") // Default to 24 hours
    private long sessionExpirySeconds;
    
    @Value("${session.management.enabled:true}") // Enable/disable session creation
    private boolean sessionManagementEnabled;
    
    private final PasetoTokenProvider tokenProvider;
    private final PasetoTokenRepository tokenRepository;
    private final SessionRepository sessionRepository;
    
    @Autowired
    public SessionAuthBusiness(PasetoTokenProvider tokenProvider, 
                              PasetoTokenRepository tokenRepository,
                              SessionRepository sessionRepository) {
        this.tokenProvider = tokenProvider;
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
    }
    
    /**
     * Create authentication tokens AND delegate session creation to data layer
     * This is the main method all service layer authenticators should call
     * 
     * Flow:
     * 1. Create PASETO tokens (business logic)
     * 2. Store tokens in PasetoTokenRepository 
     * 3. Delegate session creation to data-session module
     * 
     * @param authRequest Authentication request with all necessary context
     * @return AuthResult with tokens and optional session
     */
    public AuthResult createAuthentication(AuthRequest authRequest) {
        String acr = tokenProvider.calculateAcr(authRequest.authenticationMethods(), authRequest.isPartialAuth());
        
        log.info("Creating unified auth for user: {} with ACR: {} and methods: {}", 
                authRequest.userId(), acr, authRequest.authenticationMethods());
        
        // 1. Create PASETO tokens
        Duration accessValidity = Duration.ofHours(24);
        String accessToken = tokenProvider.createAuthenticationToken(
            authRequest.userId(), 
            authRequest.authenticationMethods(), 
            acr, 
            accessValidity,
            authRequest.tradingMode()
        );
        String refreshToken = tokenProvider.createRefreshToken(authRequest.userId());
        
        // 2. Store tokens
        storePasetoTokens(accessToken, refreshToken, authRequest);
        
        // 3. Delegate session creation to data-session module if enabled
        UserSession session = null;
        if (sessionManagementEnabled) {
            session = delegateSessionCreation(authRequest, accessToken, acr);
        }
        
        log.info("Successfully created unified auth for user: {} with {} methods (session: {})", 
                authRequest.userId(), authRequest.authenticationMethods().size(), 
                session != null ? "created" : "disabled");
        
        return new AuthResult(accessToken, refreshToken, session);
    }
    
    /**
     * Store PASETO tokens with metadata
     */
    private void storePasetoTokens(String accessToken, String refreshToken, AuthRequest authRequest) {
        Map<String, Object> accessClaims = tokenProvider.parseToken(accessToken);
        Map<String, Object> refreshClaims = tokenProvider.parseToken(refreshToken);
        
        Map<String, Object> accessTokenMetadata = new HashMap<>();
        accessTokenMetadata.put("amr", accessClaims.get("amr"));
        accessTokenMetadata.put("acr", accessClaims.get("acr"));
        accessTokenMetadata.put("auth_methods", String.join(",", authRequest.authenticationMethods()));
        accessTokenMetadata.put("scope", accessClaims.get("scope"));
        accessTokenMetadata.put("tradingMode", authRequest.tradingMode());
        
        PasetoTokenEntity accessTokenEntity = new PasetoTokenEntity();
        accessTokenEntity.setTokenId(UUID.randomUUID().toString());
        accessTokenEntity.setUserId(authRequest.userId());
        accessTokenEntity.setTokenType("ACCESS");
        accessTokenEntity.setTokenValue(accessToken);
        accessTokenEntity.setIssuedAt(Instant.ofEpochSecond(getClaimAsLong(accessClaims, "iat")));
        accessTokenEntity.setExpiresAt(Instant.ofEpochSecond(getClaimAsLong(accessClaims, "exp")));
        accessTokenEntity.setDeviceId(authRequest.deviceId());
        accessTokenEntity.setIssuedFrom(authRequest.ipAddress());
        accessTokenEntity.setRevoked(false);
        accessTokenEntity.setClaims(accessTokenMetadata);
        
        PasetoTokenEntity refreshTokenEntity = new PasetoTokenEntity();
        refreshTokenEntity.setTokenId(UUID.randomUUID().toString());
        refreshTokenEntity.setUserId(authRequest.userId());
        refreshTokenEntity.setTokenType("REFRESH");
        refreshTokenEntity.setTokenValue(refreshToken);
        refreshTokenEntity.setIssuedAt(Instant.ofEpochSecond(getClaimAsLong(refreshClaims, "iat")));
        refreshTokenEntity.setExpiresAt(Instant.ofEpochSecond(getClaimAsLong(refreshClaims, "exp")));
        refreshTokenEntity.setDeviceId(authRequest.deviceId());
        refreshTokenEntity.setIssuedFrom(authRequest.ipAddress());
        refreshTokenEntity.setRevoked(false);
        refreshTokenEntity.setClaims(Map.of("associated_access_token", accessTokenEntity.getTokenId()));
        
        tokenRepository.save(accessTokenEntity);
        tokenRepository.save(refreshTokenEntity);
    }
    
    /**
     * Delegate session creation to data-session module
     * Business layer builds the session data, data layer handles persistence
     */
    private UserSession delegateSessionCreation(AuthRequest authRequest, String accessToken, String acr) {
        try {
            String sessionId = "sess_" + UUID.randomUUID().toString();
            
            UserSessionEntity sessionEntity = new UserSessionEntity(authRequest.userId());
            sessionEntity.setSessionId(sessionId);
            sessionEntity.setUserEmail(authRequest.userEmail() != null ? authRequest.userEmail() : "unknown@example.com");
            // Store access token reference in attributes
            if (sessionEntity.getAttributes() == null) {
                sessionEntity.setAttributes(new HashMap<>());
            }
            sessionEntity.getAttributes().put("accessToken", accessToken);
            sessionEntity.setAcr(acr);
            sessionEntity.setAmr(authRequest.authenticationMethods());
            sessionEntity.setDeviceFingerprint(authRequest.deviceFingerprint() != null ? authRequest.deviceFingerprint() : "unknown");
            sessionEntity.setIpAddress(authRequest.ipAddress() != null ? authRequest.ipAddress() : "unknown");
            sessionEntity.setUserAgent(authRequest.userAgent() != null ? authRequest.userAgent() : "Unknown");
            sessionEntity.setActive(true);
            sessionEntity.setLastAccessedAt(Instant.now());
            sessionEntity.setExpiresAt(Instant.now().plusSeconds(sessionExpirySeconds));
            
            // Delegate to data-session module for persistence
            UserSessionEntity savedSessionEntity = sessionRepository.save(sessionEntity, authRequest.userId());
            UserSession savedSession = convertEntityToSession(savedSessionEntity);
            log.debug("Delegated UserSession creation {} for user {} to data layer", sessionId, authRequest.userId());
            
            return savedSession;
        } catch (Exception e) {
            log.error("Failed to create UserSession for user {}: {}", authRequest.userId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate access token by delegating to data layer
     * Business layer orchestrates, data layer provides session data
     */
    public Optional<SessionValidationResult> validateToken(String accessToken) {
        try {
            // Validate token format and signature
            if (!tokenProvider.isValidAccessToken(accessToken)) {
                return Optional.empty();
            }
            
            // Check if token exists and is not revoked
            Optional<PasetoTokenEntity> storedToken = tokenRepository.findByTokenValue(accessToken);
            if (storedToken.isEmpty() || !storedToken.get().isValid()) {
                return Optional.empty();
            }
            
            String userId = storedToken.get().getUserId();
            
            // Find associated session by access token in attributes
            List<UserSessionEntity> userSessionEntities = sessionRepository.findByUserIdAndActive(userId, true);
            Optional<UserSessionEntity> sessionEntityOpt = userSessionEntities.stream()
                .filter(s -> accessToken.equals(s.getAttributes() != null ? s.getAttributes().get("accessToken") : null))
                .findFirst();
                
            if (sessionEntityOpt.isEmpty()) {
                log.debug("No session found for valid access token");
                return createValidationResultFromToken(accessToken, userId);
            }
            
            UserSessionEntity sessionEntity = sessionEntityOpt.get();
            if (!sessionEntity.isValid()) {
                return Optional.empty();
            }
            
            // Delegate session update to data layer
            sessionEntity.updateLastAccessed();
            sessionRepository.save(sessionEntity, userId);
            
            UserSession session = convertEntityToSession(sessionEntity);
            
            // Extract trading mode from token claims
            Map<String, Object> tokenClaims = tokenProvider.parseToken(accessToken);
            String tradingMode = (String) tokenClaims.getOrDefault("tradingMode", "demo");
            
            return Optional.of(new SessionValidationResult(
                session.getUserId(),
                session.getUserEmail(),
                session.getSessionId(),
                session.getAcr(),
                session.getAmr(),
                tradingMode,
                session.getLastAccessedAt(),
                session.getExpiresAt(),
                true
            ));
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Create validation result from token when no session exists
     */
    private Optional<SessionValidationResult> createValidationResultFromToken(String accessToken, String userId) {
        try {
            Map<String, Object> claims = tokenProvider.parseToken(accessToken);
            String acr = (String) claims.getOrDefault("acr", "1");
            
            // Decode AMR from token
            @SuppressWarnings("unchecked")
            List<Integer> amrList = (List<Integer>) claims.get("amr");
            List<String> amr = decodeAuthenticationMethods(amrList);
            
            Instant issuedAt = Instant.ofEpochSecond(getClaimAsLong(claims, "iat"));
            Instant expiresAt = Instant.ofEpochSecond(getClaimAsLong(claims, "exp"));
            String tradingMode = (String) claims.getOrDefault("tradingMode", "demo");
            
            return Optional.of(new SessionValidationResult(
                userId,
                "unknown@example.com",
                "token_" + accessToken.substring(0, 8),
                acr,
                amr,
                tradingMode,
                issuedAt,
                expiresAt,
                true
            ));
        } catch (Exception e) {
            log.error("Error creating validation result from token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Revoke token and delegate session termination to data layer
     */
    public boolean revokeAuthentication(String accessToken) {
        try {
            // Revoke the token
            boolean tokenRevoked = revokeToken(accessToken);
            
            // Terminate associated session  
            Optional<PasetoTokenEntity> tokenOpt = tokenRepository.findByTokenValue(accessToken);
            if (tokenOpt.isPresent()) {
                String userId = tokenOpt.get().getUserId();
                List<UserSessionEntity> userSessionEntities = sessionRepository.findByUserIdAndActive(userId, true);
                // Delegate session termination to data layer
                userSessionEntities.stream()
                    .filter(s -> accessToken.equals(s.getAttributes() != null ? s.getAttributes().get("accessToken") : null))
                    .findFirst()
                    .ifPresent(sessionEntity -> {
                        sessionEntity.terminate("Token revoked");
                        sessionRepository.save(sessionEntity, userId);
                    });
            }
            
            return tokenRevoked;
        } catch (Exception e) {
            log.error("Error revoking authentication: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Revoke all authentication and delegate session cleanup to data layer
     */
    public int revokeAllUserAuthentication(String userId, String reason) {
        try {
            // Revoke all tokens
            int tokensRevoked = revokeAllUserTokens(userId);
            
            // Delegate session termination to data layer
            List<UserSessionEntity> sessionEntities = sessionRepository.findByUserIdAndActive(userId, true);
            for (UserSessionEntity sessionEntity : sessionEntities) {
                sessionEntity.terminate(reason);
                sessionRepository.save(sessionEntity, userId);
            }
            
            log.info("Revoked all authentication for user {}: {} tokens, {} sessions", 
                    userId, tokensRevoked, sessionEntities.size());
            return Math.max(tokensRevoked, sessionEntities.size());
        } catch (Exception e) {
            log.error("Error revoking all authentication for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Revoke a specific token
     */
    private boolean revokeToken(String token) {
        try {
            Optional<PasetoTokenEntity> storedToken = tokenRepository.findByTokenValue(token);
            if (storedToken.isEmpty()) {
                return false;
            }
            
            PasetoTokenEntity tokenEntity = storedToken.get();
            tokenEntity.setRevoked(true);
            tokenEntity.setRevokedAt(Instant.now());
            tokenEntity.setRevocationReason("Manually revoked");
            
            tokenRepository.save(tokenEntity);
            log.info("Revoked token: {}", tokenEntity.getTokenId());
            
            return true;
        } catch (Exception e) {
            log.error("Error revoking token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Revoke all tokens for a user
     */
    private int revokeAllUserTokens(String userId) {
        List<PasetoTokenEntity> tokens = tokenRepository.findByUserId(userId);
        
        int count = 0;
        for (PasetoTokenEntity token : tokens) {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
                token.setRevocationReason("All user tokens revoked");
                
                tokenRepository.save(token);
                count++;
            }
        }
        
        log.info("Revoked {} tokens for user: {}", count, userId);
        return count;
    }
    
    /**
     * Decode authentication methods from AMR
     */
    private List<String> decodeAuthenticationMethods(List<Integer> amr) {
        if (amr == null || amr.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<Integer, String> methodMap = Map.of(
            1, "password",
            2, "sms_otp", 
            3, "passkeys",
            4, "totp",
            5, "email_otp",
            6, "backup_codes"
        );
        
        List<String> methods = new ArrayList<>();
        for (Integer methodId : amr) {
            if (methodMap.containsKey(methodId)) {
                methods.add(methodMap.get(methodId));
            }
        }
        
        return methods;
    }
    
    /**
     * Helper method to parse claims as long
     */
    private long getClaimAsLong(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            String strValue = (String) value;
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                try {
                    return Instant.parse(strValue).getEpochSecond();
                } catch (Exception dateException) {
                    log.warn("Failed to parse claim {} ({}): {}", key, strValue, dateException.getMessage());
                    return Instant.now().getEpochSecond();
                }
            }
        } else {
            log.warn("Unexpected claim type for {}: {}", key, value != null ? value.getClass() : "null");
            return Instant.now().getEpochSecond();
        }
    }
    
    /**
     * Legacy method for compatibility with TokenSessionService
     * Creates authentication token pair using the new unified approach
     */
    public TokenPair createAuthenticationTokenPair(String userId, List<String> authMethods, String acr, String deviceId, String ipAddress) {
        AuthRequest authRequest = new AuthRequest(userId, null, authMethods, false, deviceId, deviceId, ipAddress, "Legacy Client", "live");
        AuthResult result = createAuthentication(authRequest);
        return new TokenPair(result.accessToken(), result.refreshToken());
    }
    
    /**
     * Legacy method for compatibility - validate session token
     */
    public Optional<String> validateSession(String token) {
        Optional<SessionValidationResult> validation = validateToken(token);
        return validation.map(SessionValidationResult::getUserId);
    }
    
    /**
     * Legacy method for compatibility - delete session by token
     */
    public void deleteSession(String token) {
        revokeAuthentication(token);
    }
    
    /**
     * Legacy method for compatibility - delete all user sessions
     */
    public boolean deleteUserSessions(String userId) {
        int revoked = revokeAllUserAuthentication(userId, "User logout");
        return revoked > 0;
    }
    
    /**
     * Refresh access token using refresh token
     * Maintains the same ACR, AAL, and AMR from the original session
     */
    public Optional<String> refreshAccessToken(String refreshToken, String ipAddress) {
        try {
            // Find the refresh token entity
            Optional<PasetoTokenEntity> refreshTokenOpt = tokenRepository.findByTokenValue(refreshToken);
            
            if (refreshTokenOpt.isEmpty()) {
                log.warn("Refresh token not found");
                return Optional.empty();
            }
            
            PasetoTokenEntity refreshTokenEntity = refreshTokenOpt.get();
            
            // Check if refresh token is valid
            if (!refreshTokenEntity.isValid()) {
                log.warn("Refresh token is expired or revoked");
                return Optional.empty();
            }
            
            String userId = refreshTokenEntity.getUserId();
            
            // Find the associated access token to get auth context
            String associatedAccessTokenId = (String) refreshTokenEntity.getClaims().get("associated_access_token");
            if (associatedAccessTokenId == null) {
                log.warn("No associated access token found for refresh token");
                return Optional.empty();
            }
            
            // Get the original access token to extract auth methods and context
            List<PasetoTokenEntity> userTokens = tokenRepository.findByUserId(userId);
            Optional<PasetoTokenEntity> originalAccessTokenOpt = userTokens.stream()
                .filter(t -> t.getTokenId().equals(associatedAccessTokenId))
                .findFirst();
            
            if (originalAccessTokenOpt.isEmpty()) {
                log.warn("Original access token not found");
                return Optional.empty();
            }
            
            PasetoTokenEntity originalAccessToken = originalAccessTokenOpt.get();
            Map<String, Object> originalClaims = originalAccessToken.getClaims();
            
            // Extract authentication context from original token
            String acr = (String) originalClaims.getOrDefault("acr", "1");
            String authMethodsStr = (String) originalClaims.getOrDefault("auth_methods", "password");
            List<String> authMethods = List.of(authMethodsStr.split(","));
            String tradingMode = (String) originalClaims.getOrDefault("tradingMode", "demo");
            
            // Generate new access token with same auth context
            Duration accessValidity = Duration.ofHours(24);
            String newAccessToken = tokenProvider.createAuthenticationToken(
                userId,
                authMethods,
                acr,
                accessValidity,
                tradingMode
            );
            
            // Store the new access token
            Map<String, Object> newAccessClaims = tokenProvider.parseToken(newAccessToken);
            
            Map<String, Object> accessTokenMetadata = new HashMap<>();
            accessTokenMetadata.put("amr", newAccessClaims.get("amr"));
            accessTokenMetadata.put("acr", acr);
            accessTokenMetadata.put("auth_methods", authMethodsStr);
            accessTokenMetadata.put("scope", newAccessClaims.get("scope"));
            accessTokenMetadata.put("tradingMode", tradingMode);
            
            PasetoTokenEntity newAccessTokenEntity = new PasetoTokenEntity();
            newAccessTokenEntity.setTokenId(UUID.randomUUID().toString());
            newAccessTokenEntity.setUserId(userId);
            newAccessTokenEntity.setTokenType("ACCESS");
            newAccessTokenEntity.setTokenValue(newAccessToken);
            newAccessTokenEntity.setIssuedAt(Instant.ofEpochSecond(getClaimAsLong(newAccessClaims, "iat")));
            newAccessTokenEntity.setExpiresAt(Instant.ofEpochSecond(getClaimAsLong(newAccessClaims, "exp")));
            newAccessTokenEntity.setDeviceId(refreshTokenEntity.getDeviceId());
            newAccessTokenEntity.setIssuedFrom(ipAddress != null ? ipAddress : refreshTokenEntity.getIssuedFrom());
            newAccessTokenEntity.setRevoked(false);
            newAccessTokenEntity.setClaims(accessTokenMetadata);
            
            tokenRepository.save(newAccessTokenEntity);
            
            // Update the refresh token to point to the new access token
            refreshTokenEntity.getClaims().put("associated_access_token", newAccessTokenEntity.getTokenId());
            tokenRepository.save(refreshTokenEntity);
            
            log.info("Successfully refreshed access token for user {}", userId);
            return Optional.of(newAccessToken);
            
        } catch (Exception e) {
            log.error("Error refreshing access token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Add an authentication method to an existing session and update tokens
     * Used when adding 2FA during an active session (e.g., TOTP registration)
     * 
     * @param sessionToken Current session token
     * @param authMethod New authentication method to add
     * @param newAcrLevel New ACR level after adding the method
     * @return Map with updated tokens
     */
    public Map<String, Object> addAuthenticationMethod(String sessionToken, String authMethod, int newAcrLevel) {
        try {
            // Parse the current token to get user and existing methods
            Map<String, Object> currentClaims = tokenProvider.parseToken(sessionToken);
            String userId = tokenProvider.getUserIdFromToken(sessionToken);
            
            // Decode existing AMR
            @SuppressWarnings("unchecked")
            List<Integer> amrList = (List<Integer>) currentClaims.get("amr");
            List<String> existingMethods = decodeAuthenticationMethods(amrList);
            
            // Add the new method
            List<String> updatedMethods = new ArrayList<>(existingMethods);
            if (!updatedMethods.contains(authMethod)) {
                updatedMethods.add(authMethod);
            }
            
            // Preserve trading mode from current token
            String tradingMode = (String) currentClaims.getOrDefault("tradingMode", "demo");
            
            // Create new tokens with updated authentication context
            Duration accessValidity = Duration.ofHours(24);
            String newAccessToken = tokenProvider.createAuthenticationToken(
                userId,
                updatedMethods,
                String.valueOf(newAcrLevel),
                accessValidity,
                tradingMode
            );
            String newRefreshToken = tokenProvider.createRefreshToken(userId);
            
            // Store the new tokens
            AuthRequest authRequest = new AuthRequest(
                userId,
                null,
                updatedMethods,
                false,
                null,
                null,
                null,
                null,
                tradingMode  // Preserve trading mode
            );
            storePasetoTokens(newAccessToken, newRefreshToken, authRequest);
            
            // Return the updated tokens
            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("refreshToken", newRefreshToken);
            result.put("identityToken", newAccessToken); // For backward compatibility
            result.put("success", true);
            
            log.info("Added authentication method '{}' for user {} with new ACR level {}", 
                     authMethod, userId, newAcrLevel);
            
            return result;
        } catch (PasetoException e) {
            log.error("Failed to add authentication method: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Legacy method for compatibility - cleanup expired tokens
     */
    public void cleanupExpiredTokens() {
        cleanupExpiredAuth();
    }
    
    /**
     * Cleanup expired tokens and delegate session cleanup to data layer
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredAuth() {
        log.info("Cleaning up expired tokens and sessions");
        
        // Clean up expired tokens
        Instant now = Instant.now();
        List<PasetoTokenEntity> expiredTokens = tokenRepository.findByExpiresAtBefore(now);
        for (PasetoTokenEntity token : expiredTokens) {
            tokenRepository.delete(token);
        }
        int expiredTokenCount = expiredTokens.size();
        
        // Delegate session cleanup to data layer
        int expiredSessions = sessionRepository.cleanupExpiredSessions(Instant.now());
        
        log.info("Business layer coordinated cleanup: {} expired tokens, {} expired sessions", expiredTokenCount, expiredSessions);
    }
    
    /**
     * Convert UserSessionEntity to UserSession domain model
     */
    private UserSession convertEntityToSession(UserSessionEntity entity) {
        UserSession session = new UserSession();
        session.setSessionId(entity.getSessionId());
        session.setUserId(entity.getUserId());
        session.setUserEmail(entity.getUserEmail());
        session.setCreatedAt(entity.getCreatedAt());
        session.setLastAccessedAt(entity.getLastAccessedAt());
        session.setExpiresAt(entity.getExpiresAt());
        session.setIpAddress(entity.getIpAddress());
        session.setUserAgent(entity.getUserAgent());
        session.setDeviceFingerprint(entity.getDeviceFingerprint());
        session.setAcr(entity.getAcr());
        session.setAmr(entity.getAmr());
        session.setAuthorities(entity.getAuthorities());
        session.setAttributes(entity.getAttributes());
        session.setActive(Boolean.TRUE.equals(entity.getIsActive()));
        session.setTerminationReason(entity.getTerminationReason());
        return session;
    }
    
    /**
     * Token pair record for legacy compatibility
     */
    public record TokenPair(String accessToken, String refreshToken) {}
    
    /**
     * Authentication request record - pure data, no servlet dependencies
     */
    public record AuthRequest(
        String userId,
        String userEmail,
        List<String> authenticationMethods,
        boolean isPartialAuth,
        String deviceId,
        String deviceFingerprint,
        String ipAddress,
        String userAgent,
        String tradingMode  // "demo" or "live"
    ) {}
    
    /**
     * Authentication result record - tokens and optional session
     */
    public record AuthResult(
        String accessToken,
        String refreshToken,
        UserSession session
    ) {
        public boolean hasSession() {
            return session != null;
        }
        
        public String getSessionId() {
            return session != null ? session.getSessionId() : null;
        }
    }
}