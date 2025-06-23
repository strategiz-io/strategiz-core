package io.strategiz.service.auth.service.passkey;

import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasskeyManagementServiceTest {

    private PasskeyManagementService passkeyManagementService;
    
    @Mock
    private PasskeyCredentialRepository credentialRepository;
    
    @BeforeEach
    public void setUp() {
        passkeyManagementService = new PasskeyManagementService(credentialRepository);
    }
    
    @Test
    public void testGetPasskeysForUser() {
        // Given
        String userId = UUID.randomUUID().toString();
        
        Instant now = Instant.now();
        PasskeyCredential credential1 = mock(PasskeyCredential.class);
        when(credential1.getCredentialId()).thenReturn("cred-id-1");
        when(credential1.getAuthenticatorName()).thenReturn("Passkey 1");
        when(credential1.getRegistrationTime()).thenReturn(now.minus(10, ChronoUnit.DAYS));
        when(credential1.getLastUsedTime()).thenReturn(now.minus(1, ChronoUnit.HOURS));
        
        PasskeyCredential credential2 = mock(PasskeyCredential.class);
        when(credential2.getCredentialId()).thenReturn("cred-id-2");
        when(credential2.getAuthenticatorName()).thenReturn("Passkey 2");
        when(credential2.getRegistrationTime()).thenReturn(now.minus(5, ChronoUnit.DAYS));
        when(credential2.getLastUsedTime()).thenReturn(now.minus(5, ChronoUnit.HOURS));
        
        List<PasskeyCredential> mockCredentials = Arrays.asList(credential1, credential2);
        when(credentialRepository.findByUserId(userId)).thenReturn(mockCredentials);
        
        // When
        List<PasskeyManagementService.PasskeyDetails> result = passkeyManagementService.getPasskeysForUser(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        assertEquals("cred-id-1", result.get(0).id());
        assertEquals("Passkey 1", result.get(0).name());
        assertNotNull(result.get(0).registeredAt());
        assertNotNull(result.get(0).lastUsedAt());
        
        assertEquals("cred-id-2", result.get(1).id());
        assertEquals("Passkey 2", result.get(1).name());
        
        // Verify
        verify(credentialRepository, times(1)).findByUserId(userId);
    }
    
    @Test
    public void testDeletePasskey_Success() {
        // Given
        String userId = UUID.randomUUID().toString();
        String credentialId = "cred-id-1";
        
        PasskeyCredential credential = mock(PasskeyCredential.class);
        when(credential.getId()).thenReturn(credentialId);
        when(credential.getUserId()).thenReturn(userId);
        
        when(credentialRepository.findByCredentialId(credentialId)).thenReturn(Optional.of(credential));
        
        // When
        boolean result = passkeyManagementService.deletePasskey(userId, credentialId);
        
        // Then
        assertTrue(result);
        
        // Verify
        verify(credentialRepository, times(1)).findByCredentialId(credentialId);
        verify(credentialRepository, times(1)).delete(credential);
    }
    
    @Test
    public void testDeletePasskey_CredentialNotFound() {
        // Given
        String userId = UUID.randomUUID().toString();
        String credentialId = "non-existent-id";
        
        when(credentialRepository.findByCredentialId(credentialId)).thenReturn(Optional.empty());
        
        // When
        boolean result = passkeyManagementService.deletePasskey(userId, credentialId);
        
        // Then
        assertFalse(result);
        
        // Verify
        verify(credentialRepository, times(1)).findByCredentialId(credentialId);
        verify(credentialRepository, never()).delete(any());
    }
    
    @Test
    public void testDeletePasskey_WrongUser() {
        // Given
        String userId = UUID.randomUUID().toString();
        String wrongUserId = UUID.randomUUID().toString();
        String credentialId = "cred-id-1";
        
        PasskeyCredential credential = mock(PasskeyCredential.class);
        when(credential.getCredentialId()).thenReturn(credentialId);
        when(credential.getUserId()).thenReturn(wrongUserId); // Different user ID
        
        when(credentialRepository.findByCredentialId(credentialId)).thenReturn(Optional.of(credential));
        
        // When
        boolean result = passkeyManagementService.deletePasskey(userId, credentialId);
        
        // Then
        assertFalse(result);
        
        // Verify
        verify(credentialRepository, times(1)).findByCredentialId(credentialId);
        verify(credentialRepository, never()).delete(any());
    }
}
