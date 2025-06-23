package io.strategiz.service.auth.service.passkey;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.business.tokenauth.SessionAuthBusiness.TokenPair;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.auth.repository.passkey.challenge.PasskeyChallengeRepository;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import io.strategiz.service.auth.service.passkey.PasskeyAuthenticationService.AuthenticationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class PasskeyAuthenticationServiceTest {

    private PasskeyAuthenticationService authenticationService;
    
    @Mock
    private PasskeyChallengeService challengeService;
    
    @Mock
    private PasskeyCredentialRepository credentialRepository;
    
    @Mock
    private SessionAuthBusiness sessionAuthBusiness;
    
    @BeforeEach
    public void setUp() {
        authenticationService = new PasskeyAuthenticationService(
            challengeService, 
            credentialRepository, 
            sessionAuthBusiness
        );
        
        // Set properties using reflection
        ReflectionTestUtils.setField(authenticationService, "rpId", "localhost");
        ReflectionTestUtils.setField(authenticationService, "rpName", "Strategiz Test");
        ReflectionTestUtils.setField(authenticationService, "challengeTimeoutMs", 60000);
    }
    
    @Test
    public void testBeginAuthentication() {
        // Given
        when(challengeService.createChallenge(anyString(), any(PasskeyChallengeType.class)))
            .thenReturn("challenge123");
            
        // When
        PasskeyAuthenticationService.AuthenticationChallenge challenge = authenticationService.beginAuthentication();
        
        // Then
        assertNotNull(challenge);
        assertEquals("localhost", challenge.rpId());
        assertNotNull(challenge.challenge());
        assertNotNull(challenge.timeout());
        
        verify(challengeService, times(1)).createChallenge(anyString(), eq(PasskeyChallengeType.AUTHENTICATION));
    }
    
    @Test
    public void testCompleteAuthentication_Success() {
        // Given
        String userId = UUID.randomUUID().toString();
        PasskeyCredential credential = mock(PasskeyCredential.class);
        when(credential.getUserId()).thenReturn(userId);
        
        when(credentialRepository.findByCredentialId(anyString())).thenReturn(Optional.of(credential));
        
        // Mock validation
        when(challengeService.verifyChallenge(anyString(), anyString(), any(PasskeyChallengeType.class))).thenReturn(true);
        
        // Mock challenge extraction
        when(challengeService.extractChallengeFromClientData(anyString())).thenReturn("challenge123");
        
        // Mock token generation with createTokenPair which we know exists in the API
        TokenPair mockTokenPair = new TokenPair("access-token-123", "refresh-token-123");
        when(sessionAuthBusiness.createTokenPair(anyString(), anyString(), anyString(), any(String[].class))).thenReturn(mockTokenPair);
        
        // Create completion request
        PasskeyAuthenticationService.AuthenticationCompletion completion = 
            new PasskeyAuthenticationService.AuthenticationCompletion(
                "credential123", "authenticator-data", "client-data", 
                "signature", userId, "127.0.0.1", "device123");
        
        // When
        AuthenticationResult result = authenticationService.completeAuthentication(completion);
        
        // Then
        assertTrue(result.success());
        assertEquals("access-token-123", result.accessToken());
        assertEquals("refresh-token-123", result.refreshToken());
        assertNull(result.errorMessage());
        
        // Verify
        verify(credentialRepository, times(1)).findByCredentialId(anyString());
        verify(challengeService, times(1)).extractChallengeFromClientData(anyString());
        verify(challengeService, times(1)).verifyChallenge(anyString(), anyString(), any(PasskeyChallengeType.class));
        verify(sessionAuthBusiness, times(1)).createTokenPair(anyString(), anyString(), anyString(), any(String[].class));
    }
    
    @Test
    public void testCompleteAuthentication_CredentialNotFound() {
        // Given
        when(credentialRepository.findByCredentialId(anyString())).thenReturn(Optional.empty());
        
        // Create completion request
        PasskeyAuthenticationService.AuthenticationCompletion completion = 
            new PasskeyAuthenticationService.AuthenticationCompletion(
                "invalid-credential", "authenticator-data", "client-data", 
                "signature", "user123", "127.0.0.1", "device123");
        
        // When
        AuthenticationResult result = authenticationService.completeAuthentication(completion);
        
        // Then
        assertFalse(result.success());
        assertNull(result.accessToken());
        assertNull(result.refreshToken());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("Credential not found"));
        
        // Verify
        verify(credentialRepository, times(1)).findByCredentialId(anyString());
        verify(challengeService, never()).verifyChallenge(anyString(), anyString(), any(PasskeyChallengeType.class));
        verify(sessionAuthBusiness, never()).generateToken(anyString());
    }
}
