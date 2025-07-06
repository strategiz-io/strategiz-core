package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.ReadProviderRequest;
import io.strategiz.service.provider.model.response.ReadProviderResponse;
import io.strategiz.service.provider.service.ReadProviderService;
import io.strategiz.service.base.controller.ProviderBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

/**
 * Controller for reading provider connections and data.
 * Handles retrieving provider status, balances, transactions, and connection information.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers")
public class ReadProviderController extends ProviderBaseController {
    
    private final ReadProviderService readProviderService;
    
    @Autowired
    public ReadProviderController(ReadProviderService readProviderService) {
        this.readProviderService = readProviderService;
    }
    
    /**
     * Gets all provider connections for a user.
     * 
     * @param principal The authenticated user principal
     * @param status Optional status filter (connected, disconnected, pending, error)
     * @param page Optional page number for pagination
     * @param limit Optional page size for pagination
     * @return ReadProviderResponse containing list of provider connections
     */
    @GetMapping
    public ResponseEntity<ReadProviderResponse> getProviders(
            Principal principal,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        
        // Extract user ID from authentication principal
        String userId = extractUserId(principal);
        
        providerLog.info("Getting providers for user: {}, status: {}, page: {}, limit: {}", 
                        userId, status, page, limit);
        
        try {
            // Log the read attempt
            logProviderAttempt(userId, "READ_PROVIDERS", false);
            
            ReadProviderRequest request = new ReadProviderRequest();
            request.setUserId(userId);
            request.setStatus(status);
            request.setPage(page);
            request.setLimit(limit);
            
            ReadProviderResponse response = readProviderService.getProviders(request);
            
            // Log successful attempt
            logProviderAttempt(userId, "READ_PROVIDERS", true);
            
            providerLog.info("Retrieved {} providers for user: {}", 
                           response.getTotalCount(), userId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            providerLog.warn("Invalid provider read request for user: {}: {}", userId, e.getMessage());
            
            ReadProviderResponse errorResponse = new ReadProviderResponse();
            errorResponse.setStatus("error");
            errorResponse.setErrorCode("INVALID_REQUEST");
            errorResponse.setErrorMessage("Invalid request: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            providerLog.error("Error retrieving providers for user: {}", userId, e);
            
            // Log failed attempt
            logProviderAttempt(userId, "READ_PROVIDERS", false);
            
            ReadProviderResponse errorResponse = new ReadProviderResponse();
            errorResponse.setStatus("error");
            errorResponse.setErrorCode("INTERNAL_ERROR");
            errorResponse.setErrorMessage("Internal server error occurred");
            errorResponse.setErrorDetails(e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Gets a specific provider connection and its data.
     * 
     * @param providerId The provider ID
     * @param principal The authenticated user principal
     * @param dataType Optional data type filter (balances, transactions, orders, positions)
     * @return ReadProviderResponse containing provider connection and data
     */
    @GetMapping("/{providerId}")
    public ResponseEntity<ReadProviderResponse> getProvider(
            @PathVariable("providerId") String providerId,
            Principal principal,
            @RequestParam(value = "dataType", required = false) String dataType) {
        
        // Extract user ID from authentication principal
        String userId = extractUserId(principal);
        
        providerLog.info("Getting provider: {} for user: {}, dataType: {}", 
                        providerId, userId, dataType);
        
        try {
            // Create request with user ID and provider ID
            ReadProviderRequest request = new ReadProviderRequest();
            request.setUserId(userId);
            request.setProviderId(providerId);
            
            // Use base class logging
            logProviderAttempt(request.getUserId(), "READ_PROVIDER", false);
            
            // Delegate to service
            ReadProviderResponse response = readProviderService.getProvider(request);
            
            // Log successful attempt
            logProviderAttempt(request.getUserId(), "READ_PROVIDER", true);
            
            providerLog.info("Retrieved provider: {} for user: {}, status: {}",
                           request.getProviderId(), request.getUserId(), response.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // Log failed attempt
            logProviderAttempt(extractUserId(principal), "READ_PROVIDER", false);
            
            providerLog.warn("Invalid provider read request for user: {}, provider: {}: {}",
                           extractUserId(principal), providerId, e.getMessage());
            
            ReadProviderResponse errorResponse = new ReadProviderResponse();
            errorResponse.setStatus("error");
            errorResponse.setErrorCode("VALIDATION_ERROR");
            errorResponse.setErrorMessage(e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            // Log failed attempt
            logProviderAttempt(extractUserId(principal), "READ_PROVIDER", false);
            
            providerLog.error("Error retrieving provider: {} for user: {}", providerId, extractUserId(principal), e);
            
            ReadProviderResponse errorResponse = new ReadProviderResponse();
            errorResponse.setStatus("error");
            errorResponse.setErrorCode("INTERNAL_ERROR");
            errorResponse.setErrorMessage("An unexpected error occurred");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Gets provider connection status only (lightweight endpoint).
     * 
     * @param providerId The provider ID
     * @param principal The authenticated user principal
     * @return ReadProviderResponse with connection status only
     */
    @GetMapping("/{providerId}/status")
    public ResponseEntity<ReadProviderResponse> getProviderStatus(
            @PathVariable("providerId") String providerId,
            Principal principal) {
        
        // Extract user ID from authentication principal
        String userId = extractUserId(principal);
        
        providerLog.info("Getting provider status for: {} for user: {}", 
                        providerId, userId);
        
        try {
            // Create request with user ID and provider ID
            ReadProviderRequest request = new ReadProviderRequest();
            request.setUserId(userId);
            request.setProviderId(providerId);
            
            // Use base class logging
            logProviderAttempt(request.getUserId(), "READ_PROVIDER", false);
            
            // Delegate to service
            ReadProviderResponse response = readProviderService.getProviderStatus(request);
            
            // Log successful attempt
            logProviderAttempt(request.getUserId(), "READ_PROVIDER", true);
            
            providerLog.info("Retrieved provider status: {} for user: {}, status: {}",
                           request.getProviderId(), request.getUserId(), response.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // Log failed attempt
            logProviderAttempt(extractUserId(principal), "READ_PROVIDER", false);
            
            providerLog.warn("Invalid provider status request for user: {}, provider: {}: {}",
                           extractUserId(principal), providerId, e.getMessage());
            
            ReadProviderResponse errorResponse = new ReadProviderResponse();
            errorResponse.setStatus("error");
            errorResponse.setErrorCode("VALIDATION_ERROR");
            errorResponse.setErrorMessage(e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            // Log failed attempt
            logProviderAttempt(extractUserId(principal), "READ_PROVIDER", false);
            
            providerLog.error("Error retrieving provider status: {} for user: {}", providerId, extractUserId(principal), e);
            
            ReadProviderResponse errorResponse = new ReadProviderResponse();
            errorResponse.setStatus("error");
            errorResponse.setErrorCode("INTERNAL_ERROR");
            errorResponse.setErrorMessage("An unexpected error occurred");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for the read provider service.
     * 
     * @return Simple health status
     */
    @GetMapping("/read/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ReadProviderController is healthy");
    }
    
    /**
     * Get the provider ID for this controller.
     * 
     * @return The provider ID
     */
    @Override
    protected String getProviderId() {
        return "provider"; // Generic provider controller
    }
    
    /**
     * Get the provider display name.
     * 
     * @return The provider display name
     */
    @Override
    protected String getProviderName() {
        return "Provider"; // Generic provider controller
    }
} 