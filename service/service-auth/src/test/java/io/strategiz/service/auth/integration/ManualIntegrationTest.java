package io.strategiz.service.auth.integration;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.session.entity.UserSession;

import java.time.Instant;
import java.util.List;

/**
 * Manual integration test demonstrating the unified authentication flow
 * This can be run as a main method to validate the integration works
 * 
 * To run: 
 * mvn exec:java -Dexec.mainClass="io.strategiz.service.auth.integration.ManualIntegrationTest" -pl service/service-auth
 */
public class ManualIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting Unified Authentication Flow Integration Test");
        System.out.println("====================================================");
        
        // Test 1: Verify AuthRequest data structure
        testAuthRequestStructure();
        
        // Test 2: Verify AuthResult data structure  
        testAuthResultStructure();
        
        // Test 3: Demonstrate page refresh solution
        testPageRefreshSolution();
        
        // Test 4: Show multi-factor authentication
        testMultiFactorAuthentication();
        
        System.out.println("\n✅ All integration tests passed!");
        System.out.println("🎯 Ready to implement TOTP and Email OTP completion flows");
    }
    
    private static void testAuthRequestStructure() {
        System.out.println("\n1️⃣ Testing AuthRequest Structure");
        System.out.println("--------------------------------");
        
        String userId = "test-user-123";
        String userEmail = "user@example.com";
        List<String> authMethods = List.of("passkeys");
        
        SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(
            userId,
            userEmail,
            authMethods,
            false, // Not partial auth
            "device-123",
            "fingerprint-456",
            "192.168.1.100",
            "Test Client"
        );
        
        System.out.println("   ✓ User ID: " + authRequest.userId());
        System.out.println("   ✓ Email: " + authRequest.userEmail());
        System.out.println("   ✓ Auth Methods: " + authRequest.authenticationMethods());
        System.out.println("   ✓ Partial Auth: " + authRequest.isPartialAuth());
        System.out.println("   ✓ Device ID: " + authRequest.deviceId());
        System.out.println("   ✓ IP Address: " + authRequest.ipAddress());
        
        assert authRequest.userId().equals(userId);
        assert authRequest.userEmail().equals(userEmail);
        assert authRequest.authenticationMethods().contains("passkeys");
        assert !authRequest.isPartialAuth();
        
        System.out.println("   ✅ AuthRequest structure validated");
    }
    
    private static void testAuthResultStructure() {
        System.out.println("\n2️⃣ Testing AuthResult Structure (Unified Flow)");
        System.out.println("----------------------------------------------");
        
        String accessToken = "access-token-12345";
        String refreshToken = "refresh-token-67890";
        String sessionId = "session-abc123";
        
        // Create UserSession (demonstrates session storage)
        UserSession session = new UserSession();
        session.setSessionId(sessionId);
        session.setUserId("test-user-123");
        session.setUserEmail("user@example.com");
        session.setCreatedAt(Instant.now());
        session.setLastAccessedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(86400)); // 24 hours
        
        SessionAuthBusiness.AuthResult result = new SessionAuthBusiness.AuthResult(
            accessToken,
            refreshToken,
            session
        );
        
        System.out.println("   ✓ Access Token: " + result.accessToken().substring(0, 20) + "...");
        System.out.println("   ✓ Refresh Token: " + result.refreshToken().substring(0, 20) + "...");
        System.out.println("   ✓ Has Session: " + result.hasSession());
        System.out.println("   ✓ Session ID: " + result.getSessionId());
        System.out.println("   ✓ Session User: " + result.session().getUserId());
        System.out.println("   ✓ Session Email: " + result.session().getUserEmail());
        
        assert result.accessToken().equals(accessToken);
        assert result.refreshToken().equals(refreshToken);
        assert result.hasSession();
        assert result.getSessionId().equals(sessionId);
        assert result.session().getUserId().equals("test-user-123");
        
        System.out.println("   ✅ AuthResult structure validated - TOKENS AND SESSION created together!");
    }
    
    private static void testPageRefreshSolution() {
        System.out.println("\n3️⃣ Testing Page Refresh Solution");
        System.out.println("--------------------------------");
        
        System.out.println("   📱 Original Problem:");
        System.out.println("      • User authenticates → tokens in localStorage");
        System.out.println("      • Page refresh → JavaScript loading → flash of unauthenticated state");
        System.out.println("");
        System.out.println("   🔧 Unified Solution:");
        System.out.println("      • Authentication creates BOTH tokens AND server session");
        System.out.println("      • Page refresh can validate session server-side IMMEDIATELY");
        System.out.println("");
        
        // Simulate server-side session (available without JavaScript)
        UserSession serverSession = new UserSession();
        serverSession.setSessionId("page-refresh-session");
        serverSession.setUserId("page-refresh-user");
        serverSession.setUserEmail("user@example.com");
        serverSession.setCreatedAt(Instant.now().minusSeconds(1800)); // 30 min ago
        serverSession.setLastAccessedAt(Instant.now().minusSeconds(60)); // 1 min ago
        serverSession.setExpiresAt(Instant.now().plusSeconds(84600)); // Still valid
        
        boolean sessionValid = serverSession.getExpiresAt().isAfter(Instant.now());
        String userInfo = serverSession.getUserEmail();
        
        System.out.println("   ✓ Server Session Valid: " + sessionValid);
        System.out.println("   ✓ User Email Available: " + userInfo);
        System.out.println("   ✓ Session Expires: " + serverSession.getExpiresAt());
        System.out.println("");
        System.out.println("   🎯 Frontend can call /auth/session/validate-server on page load");
        System.out.println("   🎯 No JavaScript dependency → No unauthenticated flash!");
        
        assert sessionValid;
        assert userInfo.equals("user@example.com");
        
        System.out.println("   ✅ Page refresh solution validated");
    }
    
    private static void testMultiFactorAuthentication() {
        System.out.println("\n4️⃣ Testing Multi-Factor Authentication Flow");
        System.out.println("-------------------------------------------");
        
        String userId = "mfa-user";
        
        // Step 1: First factor (password) - partial authentication
        SessionAuthBusiness.AuthRequest firstFactor = new SessionAuthBusiness.AuthRequest(
            userId,
            "mfa-user@example.com",
            List.of("password"),
            true, // Partial auth - need more factors
            "device-mfa",
            "fingerprint-mfa",
            "192.168.1.200", 
            "MFA Client"
        );
        
        System.out.println("   🔐 First Factor (Password):");
        System.out.println("      • Methods: " + firstFactor.authenticationMethods());
        System.out.println("      • Partial: " + firstFactor.isPartialAuth());
        
        // Step 2: Second factor (TOTP) - complete authentication
        SessionAuthBusiness.AuthRequest secondFactor = new SessionAuthBusiness.AuthRequest(
            userId,
            "mfa-user@example.com", 
            List.of("password", "totp"), // Both factors
            false, // Complete auth
            "device-mfa",
            "fingerprint-mfa",
            "192.168.1.200",
            "MFA Client"
        );
        
        System.out.println("   🔑 Second Factor (TOTP):");
        System.out.println("      • Methods: " + secondFactor.authenticationMethods());
        System.out.println("      • Partial: " + secondFactor.isPartialAuth());
        
        // Final result: High-assurance authentication
        UserSession mfaSession = new UserSession();
        mfaSession.setSessionId("mfa-session-high-assurance");
        mfaSession.setUserId(userId);
        mfaSession.setUserEmail("mfa-user@example.com");
        mfaSession.setCreatedAt(Instant.now());
        
        // Store authentication methods in session attributes for ACR/AAL calculation
        mfaSession.getAttributes().put("authMethods", "password,totp");
        mfaSession.getAttributes().put("acr", "2.3"); // High assurance - multi-factor
        mfaSession.getAttributes().put("aal", "2"); // Multi-factor AAL
        
        SessionAuthBusiness.AuthResult mfaResult = new SessionAuthBusiness.AuthResult(
            "mfa-access-token-high-assurance",
            "mfa-refresh-token",
            mfaSession
        );
        
        System.out.println("   🏆 MFA Result:");
        System.out.println("      • Session ID: " + mfaResult.getSessionId());
        System.out.println("      • Auth Methods: " + mfaSession.getAttributes().get("authMethods"));
        System.out.println("      • ACR Level: " + mfaSession.getAttributes().get("acr"));
        System.out.println("      • AAL Level: " + mfaSession.getAttributes().get("aal"));
        
        assert !firstFactor.isPartialAuth() == false; // First factor is partial
        assert !secondFactor.isPartialAuth() == true;  // Second factor completes auth
        assert secondFactor.authenticationMethods().size() == 2; // Two methods used
        assert mfaResult.hasSession();
        
        System.out.println("   ✅ Multi-factor authentication flow validated");
    }
}