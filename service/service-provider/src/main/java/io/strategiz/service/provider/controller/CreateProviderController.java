package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.CreateProviderRequest;
import io.strategiz.service.provider.model.response.CreateProviderResponse;
import io.strategiz.service.provider.service.CreateProviderService;
import io.strategiz.service.base.controller.ProviderBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;

/**
 * Controller for creating provider connections and integrations.
 * Handles OAuth initiation and API key setup for various providers.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers")
public class CreateProviderController extends ProviderBaseController {
    
    private final CreateProviderService createProviderService;
    
    @Autowired
    public CreateProviderController(CreateProviderService createProviderService) {
        this.createProviderService = createProviderService;
    }
    
    /**
     * Creates a new provider connection.
     * 
     * For OAuth providers: Returns authorization URL and state
     * For API key providers: Validates credentials and creates connection
     * 
     * @param request The provider connection request
     * @param principal The authenticated user principal
     * @return CreateProviderResponse containing connection details or OAuth URL
     */
    @PostMapping
    public ResponseEntity<CreateProviderResponse> createProvider(
            @Valid @RequestBody CreateProviderRequest request,
            Principal principal) {
        
        // Extract user ID from authentication principal
        String userId = extractUserId(principal);
        request.setUserId(userId);
        
        providerLog.info("Creating provider connection for user: {}, provider: {}, type: {}", 
                        userId, request.getProviderId(), request.getConnectionType());
        
        try {
            // Log the connection attempt
            logProviderAttempt(userId, "CREATE_CONNECTION", false);
            
            CreateProviderResponse response = createProviderService.createProvider(request);
            
            // Log successful attempt
            logProviderAttempt(userId, "CREATE_CONNECTION", true);
            
            providerLog.info("Provider connection created successfully for user: {}, provider: {}, status: {}", 
                           userId, request.getProviderId(), response.getStatus());
            
            // Return 201 Created for successful provider creation
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            providerLog.warn("Invalid provider creation request for user: {}: {}", userId, e.getMessage());
            
            CreateProviderResponse errorResponse = new CreateProviderResponse();
            errorResponse.setProviderId(request.getProviderId());
            errorResponse.setProviderName(getProviderName());
            errorResponse.setStatus("failed");
            errorResponse.setMessage("Invalid request: " + e.getMessage());
            errorResponse.setOperationSuccess(false);
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            providerLog.error("Error creating provider connection for user: {}, provider: {}", 
                            userId, request.getProviderId(), e);
            
            // Log failed attempt
            logProviderAttempt(userId, "CREATE_CONNECTION", false);
            
            CreateProviderResponse errorResponse = new CreateProviderResponse();
            errorResponse.setProviderId(request.getProviderId());
            errorResponse.setProviderName(getProviderName());
            errorResponse.setStatus("failed");
            errorResponse.setMessage("Internal server error occurred");
            errorResponse.setOperationSuccess(false);
            errorResponse.setErrorCode("INTERNAL_ERROR");
            errorResponse.setErrorMessage(e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for the create provider service.
     * 
     * @return Simple health status
     */
    @GetMapping("/create/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CreateProviderController is healthy");
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