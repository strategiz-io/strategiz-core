package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.CreateProviderRequest;
import io.strategiz.service.provider.model.response.CreateProviderResponse;
import io.strategiz.service.provider.service.CreateProviderService;
import io.strategiz.service.provider.exception.ProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateProviderController.
 * Tests provider connection creation for OAuth and API key based providers.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class CreateProviderControllerTest {
    
    @Mock
    private CreateProviderService createProviderService;
    
    @Mock
    private Principal principal;
    
    @InjectMocks
    private CreateProviderController createProviderController;
    
    private CreateProviderRequest validRequest;
    private CreateProviderResponse mockResponse;
    
    @BeforeEach
    void setUp() {
        // Setup valid request
        validRequest = new CreateProviderRequest();
        validRequest.setProviderId("coinbase");
        validRequest.setConnectionType("oauth");
        validRequest.setScope("wallet:accounts:read");
        
        // Setup mock response
        mockResponse = new CreateProviderResponse();
        mockResponse.setProviderId("coinbase");
        mockResponse.setStatus("PENDING_AUTHORIZATION");
        mockResponse.setAuthorizationUrl("https://www.coinbase.com/oauth/authorize?client_id=test");
        mockResponse.setState("random-state-123");
    }
    
    @Test
    void testCreateProvider_Success_OAuth() {
        // Given
        when(principal.getName()).thenReturn("user123");
        when(createProviderService.createProvider(any(CreateProviderRequest.class))).thenReturn(mockResponse);
        
        // When
        ResponseEntity<CreateProviderResponse> response = createProviderController.createProvider(validRequest, principal);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("coinbase", response.getBody().getProviderId());
        assertEquals("PENDING_AUTHORIZATION", response.getBody().getStatus());
        assertNotNull(response.getBody().getAuthorizationUrl());
        
        verify(createProviderService, times(1)).createProvider(any(CreateProviderRequest.class));
        assertEquals("user123", validRequest.getUserId());
    }
    
    @Test
    void testCreateProvider_Success_ApiKey() {
        // Given
        validRequest.setConnectionType("api_key");
        validRequest.setProviderId("alphavantage");
        
        CreateProviderResponse apiKeyResponse = new CreateProviderResponse();
        apiKeyResponse.setProviderId("alphavantage");
        apiKeyResponse.setStatus("ACTIVE");
        apiKeyResponse.setConnectionId("conn-456");
        
        when(principal.getName()).thenReturn("user456");
        when(createProviderService.createProvider(any(CreateProviderRequest.class))).thenReturn(apiKeyResponse);
        
        // When
        ResponseEntity<CreateProviderResponse> response = createProviderController.createProvider(validRequest, principal);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("alphavantage", response.getBody().getProviderId());
        assertEquals("ACTIVE", response.getBody().getStatus());
        assertNotNull(response.getBody().getConnectionId());
        assertNull(response.getBody().getAuthorizationUrl());
    }
    
    @Test
    void testCreateProvider_MissingProviderId_ThrowsException() {
        // Given
        validRequest.setProviderId(null);
        when(principal.getName()).thenReturn("user123");
        
        // When & Then
        StrategizException exception = assertThrows(StrategizException.class, () -> {
            createProviderController.createProvider(validRequest, principal);
        });
        
        assertEquals(ProviderErrorDetails.MISSING_REQUIRED_FIELD, 
                    exception.getErrorDetails());
        assertTrue(exception.getMessage().contains("providerId"));
    }
    
    @Test
    void testCreateProvider_EmptyProviderId_ThrowsException() {
        // Given
        validRequest.setProviderId("");
        when(principal.getName()).thenReturn("user123");
        
        // When & Then
        StrategizException exception = assertThrows(StrategizException.class, () -> {
            createProviderController.createProvider(validRequest, principal);
        });
        
        assertEquals(ProviderErrorDetails.MISSING_REQUIRED_FIELD, 
                    exception.getErrorDetails());
    }
    
    @Test
    void testCreateProvider_MissingConnectionType_ThrowsException() {
        // Given
        validRequest.setConnectionType(null);
        when(principal.getName()).thenReturn("user123");
        
        // When & Then
        StrategizException exception = assertThrows(StrategizException.class, () -> {
            createProviderController.createProvider(validRequest, principal);
        });
        
        assertEquals(ProviderErrorDetails.MISSING_REQUIRED_FIELD, 
                    exception.getErrorDetails());
        assertTrue(exception.getMessage().contains("connectionType"));
    }
    
    @Test
    void testCreateProvider_AnonymousUser() {
        // Given
        when(createProviderService.createProvider(any(CreateProviderRequest.class))).thenReturn(mockResponse);
        
        // When
        ResponseEntity<CreateProviderResponse> response = createProviderController.createProvider(validRequest, null);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("anonymous", validRequest.getUserId());
    }
    
    @Test
    void testHealthCheck() {
        // When
        ResponseEntity<String> response = createProviderController.health();
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("CreateProviderController is healthy", response.getBody());
    }
    
    @Test
    void testGetModuleName() {
        // When
        String moduleName = createProviderController.getModuleName();
        
        // Then
        assertNotNull(moduleName);
        assertEquals("service-provider", moduleName);
    }
}