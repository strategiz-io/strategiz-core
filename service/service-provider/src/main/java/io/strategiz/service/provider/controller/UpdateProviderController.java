package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.UpdateProviderRequest;
import io.strategiz.service.provider.model.response.UpdateProviderResponse;
import io.strategiz.service.provider.service.UpdateProviderService;
import io.strategiz.service.base.controller.ProviderBaseController;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller for updating provider connections and data.
 * Handles HTTP requests for OAuth completion, token refresh, and configuration updates.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers")
public class UpdateProviderController extends ProviderBaseController {
    
    @Autowired
    private UpdateProviderService updateProviderService;
    
    /**
     * Updates a provider connection.
     * 
     * @param providerId The provider ID
     * @param request The update request
     * @param principal The authenticated user
     * @return ResponseEntity with update result
     */
    @PutMapping("/{providerId}")
    public ResponseEntity<UpdateProviderResponse> updateProvider(
            @PathVariable String providerId,
            @Valid @RequestBody UpdateProviderRequest request,
            Principal principal) {
        
        try {
            // Set user ID from authenticated principal
            request.setUserId(extractUserId(principal));
            request.setProviderId(providerId);
            
            // Use base class logging
            logProviderAttempt(request.getUserId(), "UPDATE_PROVIDER", false);
            
            // Delegate to service
            UpdateProviderResponse response = updateProviderService.updateProvider(request);
            
            // Log successful attempt
            logProviderAttempt(request.getUserId(), "UPDATE_PROVIDER", true);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // Log failed attempt
            logProviderAttempt(extractUserId(principal), "UPDATE_PROVIDER", false);
            
            UpdateProviderResponse errorResponse = new UpdateProviderResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorCode("VALIDATION_ERROR");
            errorResponse.setErrorMessage(e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            // Log failed attempt
            logProviderAttempt(extractUserId(principal), "UPDATE_PROVIDER", false);
            
            UpdateProviderResponse errorResponse = new UpdateProviderResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorCode("INTERNAL_ERROR");
            errorResponse.setErrorMessage("An unexpected error occurred");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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