package io.strategiz.business.tokenauth;

import dev.paseto.jpaseto.PasetoException;
import io.strategiz.business.tokenauth.model.SessionValidationResult;
import io.strategiz.data.session.entity.SessionEntity;
import io.strategiz.data.session.repository.SessionRepository;
import io.strategiz.framework.authorization.validator.PasetoTokenValidator;
import io.strategiz.framework.token.issuer.PasetoTokenIssuer;
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
    
    @Value("${session.cleanup.expired.days:7}") // Days to keep expired sessions before deletion
    private int expiredSessionRetentionDays;
    
    @Value("${session.cleanup.revoked.days:1}") // Days to keep revoked sessions before deletion
    private int revokedSessionRetentionDays;

    private final PasetoTokenIssuer tokenIssuer;
    private final PasetoTokenValidator tokenValidator;
    private final SessionRepository sessionRepository;

    @Autowired
    public SessionAuthBusiness(PasetoTokenIssuer tokenIssuer,
                              PasetoTokenValidator tokenValidator,
                              SessionRepository sessionRepository) {
        this.tokenIssuer = tokenIssuer;
        this.tokenValidator = tokenValidator;
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
        String acr = tokenIssuer.calculateAcr(authRequest.authenticationMethods(), authRequest.isPartialAuth());

        log.info("Creating unified auth for user: {} with ACR: {} and methods: {}",
                authRequest.userId(), acr, authRequest.authenticationMethods());

        // 1. Create PASETO tokens using framework-token-issuance
        Duration accessValidity = Duration.ofHours(24);
        String accessToken = tokenIssuer.createAuthenticationToken(
            authRequest.userId(),
            authRequest.authenticationMethods(),
            acr,
            accessValidity,
            authRequest.demoMode()
        );
        String refreshToken = tokenIssuer.createRefreshToken(authRequest.userId());
        
        // 2. Store tokens as sessions in unified collection
        SessionEntity accessSession = null;
        SessionEntity refreshSession = null;
        if (sessionManagementEnabled) {
            accessSession = storeAccessTokenSession(accessToken, authRequest, acr);
            refreshSession = storeRefreshTokenSession(refreshToken, authRequest, accessSession);
        }
        
        log.info("Successfully created unified auth for user: {} with {} methods (session: {})", 
                authRequest.userId(), authRequest.authenticationMethods().size(), 
                accessSession != null ? "created" : "disabled");
        
        return new AuthResult(accessToken, refreshToken, accessSession);
    }
    
    /**
     * Store access token as session in unified collection
     */
    private SessionEntity storeAccessTokenSession(String accessToken, AuthRequest authRequest, String acr) {
        Map<String, Object> accessClaims = tokenValidator.parseToken(accessToken);
        
        Map<String, Object> sessionClaims = new HashMap<>();
        sessionClaims.put("amr", accessClaims.get("amr"));
        sessionClaims.put("acr", acr);
        sessionClaims.put("auth_methods", String.join(",", authRequest.authenticationMethods()));
        sessionClaims.put("scope", accessClaims.get("scope"));
        sessionClaims.put("demoMode", authRequest.demoMode());
        
        SessionEntity accessSession = new SessionEntity(authRequest.userId());
        accessSession.setSessionId("access_" + UUID.randomUUID().toString());
        accessSession.setUserId(authRequest.userId());
        accessSession.setTokenType("ACCESS");
        accessSession.setTokenValue(accessToken);
        accessSession.setIssuedAt(Instant.ofEpochSecond(getClaimAsLong(accessClaims, "iat")));
        accessSession.setExpiresAt(Instant.ofEpochSecond(getClaimAsLong(accessClaims, "exp")));
        accessSession.setDeviceId(authRequest.deviceId());
        accessSession.setIpAddress(authRequest.ipAddress());
        accessSession.setRevoked(false);
        accessSession.setClaims(sessionClaims);
        accessSession.setLastAccessedAt(Instant.now());
        
        return sessionRepository.save(accessSession);
    }
    
    /**
     * Store refresh token as session in unified collection
     */
    private SessionEntity storeRefreshTokenSession(String refreshToken, AuthRequest authRequest, SessionEntity accessSession) {
        Map<String, Object> refreshClaims = tokenValidator.parseToken(refreshToken);
        
        SessionEntity refreshSession = new SessionEntity(authRequest.userId());
        refreshSession.setSessionId("refresh_" + UUID.randomUUID().toString());
        refreshSession.setUserId(authRequest.userId());
        refreshSession.setTokenType("REFRESH");
        refreshSession.setTokenValue(refreshToken);
        refreshSession.setIssuedAt(Instant.ofEpochSecond(getClaimAsLong(refreshClaims, "iat")));
        refreshSession.setExpiresAt(Instant.ofEpochSecond(getClaimAsLong(refreshClaims, "exp")));
        refreshSession.setDeviceId(authRequest.deviceId());
        refreshSession.setIpAddress(authRequest.ipAddress());
        refreshSession.setRevoked(false);
        refreshSession.setClaims(Map.of("associated_access_token", accessSession.getSessionId()));
        refreshSession.setLastAccessedAt(Instant.now());
        
        return sessionRepository.save(refreshSession);
    }
    
    
    /**
     * Validate access token - FIRST LAYER: PASETO signature validation only
     *
     * This method validates the token cryptographically using PASETO.
     * If the signature is valid and the token is not expired, the token is valid.
     *
     * Session database lookup is NOT required for basic token validation.
     * The PASETO token contains all the information needed for authorization.
     */
    public Optional<SessionValidationResult> validateToken(String accessToken) {
        try {
            log.debug("Validating token starting with: {}",
                    accessToken != null && accessToken.length() > 20 ? accessToken.substring(0, 20) + "..." : "null");

            // FIRST LAYER: Validate PASETO token signature and expiration via framework-authorization
            // This is the ONLY required validation - cryptographic proof that the token is authentic
            if (!tokenValidator.isValidAccessToken(accessToken)) {
                log.warn("Token validation failed - invalid token format or signature");
                return Optional.empty();
            }

            // Token signature is valid - extract claims directly from the token
            // No database lookup required for authorization
            Map<String, Object> claims = tokenValidator.parseToken(accessToken);

            // Extract user ID from token subject
            String publicUserId = (String) claims.get("sub");

            // Decode ACR from token
            String acr = (String) claims.getOrDefault("acr", "1");

            // Decode AMR from token
            List<Integer> amrList = new ArrayList<>();
            Object amrObj = claims.get("amr");
            if (amrObj instanceof List<?>) {
                List<?> list = (List<?>) amrObj;
                for (Object item : list) {
                    if (item instanceof Integer) {
                        amrList.add((Integer) item);
                    } else if (item instanceof Number) {
                        amrList.add(((Number) item).intValue());
                    }
                }
            }
            List<String> amr = decodeAuthenticationMethods(amrList);
            if (amr.isEmpty()) {
                amr = List.of("password"); // Default fallback
            }

            // Extract other claims
            Boolean demoMode = (Boolean) claims.getOrDefault("demoMode", true);
            Instant issuedAt = Instant.ofEpochSecond(getClaimAsLong(claims, "iat"));
            Instant expiresAt = Instant.ofEpochSecond(getClaimAsLong(claims, "exp"));
            String tokenId = (String) claims.get("kid");

            log.debug("Token validated successfully via PASETO signature - user: {}, acr: {}", publicUserId, acr);

            return Optional.of(new SessionValidationResult(
                publicUserId,
                "user@example.com", // Email not stored in token for privacy
                tokenId != null ? tokenId : "token_" + accessToken.substring(0, 8),
                acr,
                amr,
                demoMode,
                issuedAt,
                expiresAt,
                true
            ));
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validate token AND check session in database (optional second layer)
     * Use this when you need session management features like:
     * - Session revocation checking
     * - Session tracking/audit
     * - Last accessed time updates
     */
    public Optional<SessionValidationResult> validateTokenWithSession(String accessToken) {
        // First validate the token cryptographically
        Optional<SessionValidationResult> tokenValidation = validateToken(accessToken);
        if (tokenValidation.isEmpty()) {
            return Optional.empty();
        }

        // Optional: Check session in database for revocation status
        Optional<SessionEntity> sessionOpt = sessionRepository.findByTokenValue(accessToken);
        if (sessionOpt.isPresent()) {
            SessionEntity session = sessionOpt.get();

            // Check if session was explicitly revoked
            if (!session.isValid()) {
                log.warn("Token is cryptographically valid but session was revoked");
                return Optional.empty();
            }

            // Update last accessed time
            session.updateLastAccessed();
            sessionRepository.save(session);
        }
        // If session not found in database, token is still valid (PASETO signature is the authority)

        return tokenValidation;
    }
    
    /**
     * Create validation result from token when no session exists
     */
    private Optional<SessionValidationResult> createValidationResultFromToken(String accessToken, String userId) {
        try {
            Map<String, Object> claims = tokenValidator.parseToken(accessToken);
            String acr = (String) claims.getOrDefault("acr", "1");
            
            // Decode AMR from token
            List<Integer> amrList = new ArrayList<>();
            Object amrObj = claims.get("amr");
            if (amrObj instanceof List<?>) {
                List<?> list = (List<?>) amrObj;
                for (Object item : list) {
                    if (item instanceof Integer) {
                        amrList.add((Integer) item);
                    } else if (item instanceof Number) {
                        amrList.add(((Number) item).intValue());
                    }
                }
            }
            List<String> amr = decodeAuthenticationMethods(amrList);
            
            Instant issuedAt = Instant.ofEpochSecond(getClaimAsLong(claims, "iat"));
            Instant expiresAt = Instant.ofEpochSecond(getClaimAsLong(claims, "exp"));
            Boolean demoMode = (Boolean) claims.getOrDefault("demoMode", true);
            
            return Optional.of(new SessionValidationResult(
                userId,
                "unknown@example.com",
                "token_" + accessToken.substring(0, 8),
                acr,
                amr,
                demoMode,
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
     * Revoke token by marking session as revoked
     */
    public boolean revokeAuthentication(String accessToken) {
        try {
            // Find and revoke the session
            Optional<SessionEntity> sessionOpt = sessionRepository.findByTokenValue(accessToken);
            if (sessionOpt.isEmpty()) {
                return false;
            }
            
            SessionEntity session = sessionOpt.get();
            session.revoke("Token revoked");
            sessionRepository.save(session);
            
            log.info("Revoked session: {}", session.getSessionId());
            return true;
        } catch (Exception e) {
            log.error("Error revoking authentication: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Revoke all authentication sessions for a user
     */
    public int revokeAllUserAuthentication(String userId, String reason) {
        try {
            // Find all active sessions for user
            List<SessionEntity> sessions = sessionRepository.findByUserIdAndRevokedFalse(userId);
            
            int count = 0;
            for (SessionEntity session : sessions) {
                session.revoke(reason);
                sessionRepository.save(session);
                count++;
            }
            
            log.info("Revoked {} sessions for user: {}", count, userId);
            return count;
        } catch (Exception e) {
            log.error("Error revoking all authentication for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }
    
    
    /**
     * Decode authentication methods from AMR
     */
    private List<String> decodeAuthenticationMethods(List<Integer> amr) {
        if (amr == null || amr.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, String> methodMap = Map.ofEntries(
            Map.entry(1, "password"),
            Map.entry(2, "sms_otp"),
            Map.entry(3, "passkeys"),
            Map.entry(4, "totp"),
            Map.entry(5, "email_otp"),
            Map.entry(6, "backup_codes"),
            Map.entry(7, "google"),
            Map.entry(8, "facebook"),
            Map.entry(9, "apple"),
            Map.entry(10, "microsoft")
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
        AuthRequest authRequest = new AuthRequest(userId, null, authMethods, false, deviceId, deviceId, ipAddress, "Legacy Client", false);
        AuthResult result = createAuthentication(authRequest);
        return new TokenPair(result.accessToken(), result.refreshToken());
    }

    /**
     * Creates an identity token for signup/profile creation flow.
     * Identity tokens use a separate identity-key for security isolation.
     *
     * Two-Phase Token Flow:
     * Phase 1 (Signup): createIdentityToken() → identity-key → scope="profile:create", acr="0"
     * Phase 2 (After Auth): createAuthenticationTokenPair() → session-key → full scopes, acr="1"+"
     *
     * @param userId the user ID (or temporary ID for signup)
     * @return TokenPair with identity token only (no refresh token for identity tokens)
     */
    public TokenPair createIdentityTokenPair(String userId) {
        log.info("Creating identity token for user: {} (signup/profile creation flow)", userId);
        String identityToken = tokenIssuer.createIdentityToken(userId);
        // Identity tokens don't have refresh tokens - they're short-lived and limited scope
        return new TokenPair(identityToken, null);
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
            // Find the refresh token session
            Optional<SessionEntity> refreshSessionOpt = sessionRepository.findByTokenValue(refreshToken);
            
            if (refreshSessionOpt.isEmpty()) {
                log.warn("Refresh token not found");
                return Optional.empty();
            }
            
            SessionEntity refreshSession = refreshSessionOpt.get();
            
            // Check if refresh token is valid
            if (!refreshSession.isValid()) {
                log.warn("Refresh token is expired or revoked");
                return Optional.empty();
            }
            
            String userId = refreshSession.getUserId();
            
            // Find the associated access token to get auth context
            String associatedAccessTokenId = (String) refreshSession.getClaims().get("associated_access_token");
            if (associatedAccessTokenId == null) {
                log.warn("No associated access token found for refresh token");
                return Optional.empty();
            }
            
            // Get the original access token session
            Optional<SessionEntity> originalAccessSessionOpt = sessionRepository.findById(associatedAccessTokenId);
            
            if (originalAccessSessionOpt.isEmpty()) {
                log.warn("Original access token session not found");
                return Optional.empty();
            }
            
            SessionEntity originalAccessSession = originalAccessSessionOpt.get();
            Map<String, Object> originalClaims = originalAccessSession.getClaims();
            
            // Extract authentication context from original token
            String acr = (String) originalClaims.getOrDefault("acr", "1");
            String authMethodsStr = (String) originalClaims.getOrDefault("auth_methods", "password");
            List<String> authMethods = List.of(authMethodsStr.split(","));
            Boolean demoMode = (Boolean) originalClaims.getOrDefault("demoMode", true);
            
            // Generate new access token with same auth context using framework-token-issuance
            Duration accessValidity = Duration.ofHours(24);
            String newAccessToken = tokenIssuer.createAuthenticationToken(
                userId,
                authMethods,
                acr,
                accessValidity,
                demoMode
            );
            
            // Store the new access token as a session
            AuthRequest authRequest = new AuthRequest(
                userId,
                null,
                authMethods,
                false,
                refreshSession.getDeviceId(),
                null,
                ipAddress != null ? ipAddress : refreshSession.getIpAddress(),
                null,
                demoMode
            );
            
            SessionEntity newAccessSession = storeAccessTokenSession(newAccessToken, authRequest, acr);
            
            // Update the refresh token to point to the new access token
            refreshSession.getClaims().put("associated_access_token", newAccessSession.getSessionId());
            sessionRepository.save(refreshSession);
            
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
            // Parse the current token to get user and existing methods using framework-authorization
            Map<String, Object> currentClaims = tokenValidator.parseToken(sessionToken);
            String userId = tokenValidator.getUserIdFromToken(sessionToken);
            
            // Decode existing AMR
            List<Integer> amrList = new ArrayList<>();
            Object amrObj = currentClaims.get("amr");
            if (amrObj instanceof List<?>) {
                List<?> list = (List<?>) amrObj;
                for (Object item : list) {
                    if (item instanceof Integer) {
                        amrList.add((Integer) item);
                    } else if (item instanceof Number) {
                        amrList.add(((Number) item).intValue());
                    }
                }
            }
            List<String> existingMethods = decodeAuthenticationMethods(amrList);
            
            // Add the new method
            List<String> updatedMethods = new ArrayList<>(existingMethods);
            if (!updatedMethods.contains(authMethod)) {
                updatedMethods.add(authMethod);
            }
            
            // Preserve demo mode from current token
            Boolean demoMode = (Boolean) currentClaims.getOrDefault("demoMode", true);

            // Create new tokens with updated authentication context using framework-token-issuance
            Duration accessValidity = Duration.ofHours(24);
            String newAccessToken = tokenIssuer.createAuthenticationToken(
                userId,
                updatedMethods,
                String.valueOf(newAcrLevel),
                accessValidity,
                demoMode
            );
            String newRefreshToken = tokenIssuer.createRefreshToken(userId);
            
            // Store the new tokens as sessions
            AuthRequest authRequest = new AuthRequest(
                userId,
                null,
                updatedMethods,
                false,
                null,
                null,
                null,
                null,
                demoMode  // Preserve demo mode
            );
            SessionEntity newAccessSession = storeAccessTokenSession(newAccessToken, authRequest, String.valueOf(newAcrLevel));
            SessionEntity newRefreshSession = storeRefreshTokenSession(newRefreshToken, authRequest, newAccessSession);
            
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
     * Cleanup expired and revoked sessions based on retention policies
     * Industry standard:
     * - Expired sessions: kept for 7 days after expiration for audit
     * - Revoked sessions: kept for 1 day for immediate audit needs
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredAuth() {
        log.info("Starting session cleanup with retention policies - expired: {} days, revoked: {} days", 
                expiredSessionRetentionDays, revokedSessionRetentionDays);
        
        Instant now = Instant.now();
        
        // Clean up expired sessions older than retention period
        Instant expiredCutoff = now.minus(Duration.ofDays(expiredSessionRetentionDays));
        List<SessionEntity> expiredSessions = sessionRepository.findByExpiresAtBefore(expiredCutoff);
        int expiredCount = 0;
        for (SessionEntity session : expiredSessions) {
            // Only delete if expired for longer than retention period
            if (session.getExpiresAt().isBefore(expiredCutoff)) {
                sessionRepository.deleteById(session.getSessionId());
                expiredCount++;
            }
        }
        
        // Clean up revoked sessions older than retention period  
        List<SessionEntity> revokedSessions = sessionRepository.findByRevokedTrue();
        int revokedCount = 0;
        Instant revokedCutoff = now.minus(Duration.ofDays(revokedSessionRetentionDays));
        for (SessionEntity session : revokedSessions) {
            // Check if revoked date is older than retention period
            if (session.getRevokedAt() != null && session.getRevokedAt().isBefore(revokedCutoff)) {
                sessionRepository.deleteById(session.getSessionId());
                revokedCount++;
            }
        }
        
        log.info("Session cleanup completed - deleted {} expired sessions (older than {} days) and {} revoked sessions (older than {} days)",
                expiredCount, expiredSessionRetentionDays, revokedCount, revokedSessionRetentionDays);
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
        Boolean demoMode  // true for demo, false for live
    ) {}
    
    /**
     * Authentication result record - tokens and optional session
     */
    public record AuthResult(
        String accessToken,
        String refreshToken,
        SessionEntity session
    ) {
        public boolean hasSession() {
            return session != null;
        }
        
        public String getSessionId() {
            return session != null ? session.getSessionId() : null;
        }
    }
}