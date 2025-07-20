package io.strategiz.service.auth.controller.kraken;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.client.kraken.auth.model.KrakenApiCredentials;
import io.strategiz.client.kraken.auth.service.KrakenCredentialService;
import io.strategiz.client.kraken.auth.service.KrakenPortfolioService;
import io.strategiz.service.auth.model.kraken.KrakenApiKeyRequest;
import io.strategiz.service.auth.model.kraken.KrakenApiKeyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for KrakenAuthController
 * Tests the entire controller/service flow for Kraken provider integration
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(KrakenAuthController.class)
@AutoConfigureMockMvc
public class KrakenAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KrakenCredentialService credentialService;

    @MockBean
    private KrakenPortfolioService portfolioService;

    private static final String BASE_URL = "/v1/auth/kraken";
    private static final String TEST_USER = "test-user@example.com";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_API_SECRET = "test-api-secret-base64";
    private static final String TEST_OTP = "123456";

    private KrakenApiKeyRequest validRequest;
    private KrakenApiCredentials mockCredentials;

    @BeforeEach
    void setUp() {
        validRequest = new KrakenApiKeyRequest();
        validRequest.setApiKey(TEST_API_KEY);
        validRequest.setApiSecret(TEST_API_SECRET);
        validRequest.setOtp(TEST_OTP);

        mockCredentials = new KrakenApiCredentials();
        mockCredentials.setUserId(TEST_USER);
        mockCredentials.setApiKey(TEST_API_KEY);
        mockCredentials.setApiSecret(TEST_API_SECRET);
        mockCredentials.setOtp(TEST_OTP);
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testStoreCredentials_Success() throws Exception {
        // Given
        when(portfolioService.testConnection(any(KrakenApiCredentials.class)))
                .thenReturn(true);
        when(credentialService.storeCredentials(any(KrakenApiCredentials.class)))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/credentials")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Kraken credentials stored successfully"));

        // Verify the credentials were stored with correct user ID
        ArgumentCaptor<KrakenApiCredentials> credentialsCaptor = ArgumentCaptor.forClass(KrakenApiCredentials.class);
        verify(credentialService).storeCredentials(credentialsCaptor.capture());
        
        KrakenApiCredentials capturedCredentials = credentialsCaptor.getValue();
        assertEquals(TEST_USER, capturedCredentials.getUserId());
        assertEquals(TEST_API_KEY, capturedCredentials.getApiKey());
        assertEquals(TEST_API_SECRET, capturedCredentials.getApiSecret());
        assertEquals(TEST_OTP, capturedCredentials.getOtp());
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testStoreCredentials_InvalidCredentials() throws Exception {
        // Given
        when(portfolioService.testConnection(any(KrakenApiCredentials.class)))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/credentials")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to connect to Kraken API. Please verify your credentials."));

        // Verify credentials were NOT stored
        verify(credentialService, never()).storeCredentials(any());
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testStoreCredentials_MissingApiKey() throws Exception {
        // Given
        validRequest.setApiKey(null);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/credentials")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testTestConnection_Success() throws Exception {
        // Given
        when(credentialService.getCredentials(TEST_USER))
                .thenReturn(mockCredentials);
        when(portfolioService.testConnection(mockCredentials))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/test-connection")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Connection successful"));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testTestConnection_NoCredentials() throws Exception {
        // Given
        when(credentialService.getCredentials(TEST_USER))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/test-connection")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No Kraken credentials found"));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testGetPortfolio_Success() throws Exception {
        // Given
        Map<String, Object> mockPortfolio = new HashMap<>();
        Map<String, Object> balances = new HashMap<>();
        balances.put("BTC", "1.5");
        balances.put("ETH", "10.0");
        mockPortfolio.put("balances", balances);
        mockPortfolio.put("exchange", "KRAKEN");

        when(portfolioService.getUserPortfolio(TEST_USER))
                .thenReturn(mockPortfolio);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/portfolio")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances.BTC").value("1.5"))
                .andExpect(jsonPath("$.balances.ETH").value("10.0"))
                .andExpect(jsonPath("$.exchange").value("KRAKEN"));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testGetPortfolio_NoCredentials() throws Exception {
        // Given
        when(portfolioService.getUserPortfolio(TEST_USER))
                .thenThrow(new IllegalArgumentException("No Kraken credentials found for user"));

        // When & Then
        mockMvc.perform(get(BASE_URL + "/portfolio")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testUpdateOtp_Success() throws Exception {
        // Given
        String newOtp = "654321";
        when(credentialService.updateOtp(TEST_USER, newOtp))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(put(BASE_URL + "/otp")
                        .with(csrf())
                        .param("otp", newOtp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP updated successfully"));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testUpdateOtp_NoCredentials() throws Exception {
        // Given
        String newOtp = "654321";
        when(credentialService.updateOtp(TEST_USER, newOtp))
                .thenThrow(new IllegalArgumentException("No credentials found"));

        // When & Then
        mockMvc.perform(put(BASE_URL + "/otp")
                        .with(csrf())
                        .param("otp", newOtp))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No credentials found"));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testDeleteCredentials_Success() throws Exception {
        // Given
        when(credentialService.deleteCredentials(TEST_USER))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(delete(BASE_URL + "/credentials")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(credentialService).deleteCredentials(TEST_USER);
    }

    @Test
    void testAllEndpoints_RequireAuthentication() throws Exception {
        // Test that all endpoints require authentication
        mockMvc.perform(post(BASE_URL + "/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_URL + "/test-connection"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_URL + "/portfolio"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(BASE_URL + "/otp").param("otp", "123456"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete(BASE_URL + "/credentials"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = TEST_USER)
    void testStoreCredentials_ServiceError() throws Exception {
        // Given
        when(portfolioService.testConnection(any(KrakenApiCredentials.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(post(BASE_URL + "/credentials")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to store credentials"));
    }
}