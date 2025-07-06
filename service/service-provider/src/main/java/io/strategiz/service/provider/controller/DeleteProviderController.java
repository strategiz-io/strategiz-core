package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.DeleteProviderRequest;
import io.strategiz.service.provider.model.response.DeleteProviderResponse;
import io.strategiz.service.provider.service.DeleteProviderService;
import io.strategiz.service.base.controller.ProviderBaseController;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller for deleting provider connections and data.
 * Handles HTTP requests for disconnecting providers and cleaning up associated data.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers")
public class DeleteProviderController extends ProviderBaseController {
    
    @Autowired
    private DeleteProviderService deleteProviderService;
    
    /**
     * Deletes a provider connection.
     * 
     * @param providerId The provider ID
     * @param request The delete request (optional body with cleanup options)
     * @param principal The authenticated user
     * @return ResponseEntity with deletion result
     */
    @DeleteMapping("/{providerId}")
    public ResponseEntity<DeleteProviderResponse> deleteProvider(
            @PathVariable String providerId,
            @Valid @RequestBody(required = false) DeleteProviderRequest request,
            Principal principal) {
        
        try {
            // Create request if not provided
            if (request == null) {
                request = new DeleteProviderRequest();
            }
            
            // Set user ID from authenticated principal
            request.setUserId(extractUserId(principal));
            request.setProviderId(providerId);
            
            // Use base class logging
            logProviderAttempt(request.getUserId(), "DELETE_PROVIDER", false);
            
            // Delegate to service
            DeleteProviderResponse response = deleteProviderService.deleteProvider(request);
            
            // Log successful attempt
            logProviderAttempt(request.getUserId(), "DELETE_PROVIDER", true);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // Log failed attempt
            logProviderAttempt(extractUserId(principal), "DELETE_PROVIDER", false);
            
            DeleteProviderResponse errorResponse = new DeleteProviderResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorCode("VALIDATION_ERROR");
            errorResponse.setErrorMessage(e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            // Log failed attempt
            logProviderAttempt(extractUserId(principal), "DELETE_PROVIDER", false);
            
            DeleteProviderResponse errorResponse = new DeleteProviderResponse();
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