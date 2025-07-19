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
        String aal = tokenProvider.calculateAal(authRequest.authenticationMethods());
        
        log.info("Creating unified auth for user: {} with ACR: {} AAL: {} and methods: {}", 
                authRequest.userId(), acr, aal, authRequest.authenticationMethods());
        
        // 1. Create PASETO tokens
        Duration accessValidity = Duration.ofHours(24);
        String accessToken = tokenProvider.createAuthenticationToken(
            authRequest.userId(), 
            authRequest.authenticationMethods(), 
            acr, 
            aal, 
            accessValidity
        );
        String refreshToken = tokenProvider.createRefreshToken(authRequest.userId());
        
        // 2. Store tokens
        storePasetoTokens(accessToken, refreshToken, authRequest);
        
        // 3. Delegate session creation to data-session module if enabled
        UserSession session = null;
        if (sessionManagementEnabled) {
            session = delegateSessionCreation(authRequest, accessToken, acr, aal);
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
        accessTokenMetadata.put("aal", accessClaims.get("aal"));
        accessTokenMetadata.put("auth_methods", String.join(",", authRequest.authenticationMethods()));
        accessTokenMetadata.put("scope", accessClaims.get("scope"));
        
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
    private UserSession delegateSessionCreation(AuthRequest authRequest, String accessToken, String acr, String aal) {
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
            sessionEntity.setAal(aal);
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
            
            return Optional.of(new SessionValidationResult(
                session.getUserId(),
                session.getUserEmail(),
                session.getSessionId(),
                session.getAcr(),
                session.getAal(),
                session.getAmr(),
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
            String aal = (String) claims.getOrDefault("aal", "1");
            
            // Decode AMR from token
            @SuppressWarnings("unchecked")
            List<Integer> amrList = (List<Integer>) claims.get("amr");
            List<String> amr = decodeAuthenticationMethods(amrList);
            
            Instant issuedAt = Instant.ofEpochSecond(getClaimAsLong(claims, "iat"));
            Instant expiresAt = Instant.ofEpochSecond(getClaimAsLong(claims, "exp"));
            
            return Optional.of(new SessionValidationResult(
                userId,
                "unknown@example.com",
                "token_" + accessToken.substring(0, 8),
                acr,
                aal,
                amr,
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
        AuthRequest authRequest = new AuthRequest(userId, null, authMethods, false, deviceId, deviceId, ipAddress, "Legacy Client");
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
     * Legacy method for compatibility - refresh access token
     */
    public Optional<String> refreshAccessToken(String refreshToken, String ipAddress) {
        // For now, return empty - this would need implementation
        log.warn("refreshAccessToken not yet implemented in unified approach");
        return Optional.empty();
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
        session.setAal(entity.getAal());
        session.setAmr(entity.getAmr());
        session.setAuthorities(entity.getAuthorities());
        session.setAttributes(entity.getAttributes());
        session.setActive(entity.isActive());
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
        String userAgent
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