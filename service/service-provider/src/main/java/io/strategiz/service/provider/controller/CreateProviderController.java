package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.CreateProviderRequest;
import io.strategiz.service.provider.model.response.CreateProviderResponse;
import io.strategiz.service.provider.service.CreateProviderService;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

/**
 * Controller for creating provider connections and integrations.
 * Handles OAuth initiation and API key setup for various providers.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@RestController("providerCreateProviderController")
@RequestMapping("/v1/providers")
public class CreateProviderController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.PROVIDER_MODULE;
    }
    
    private final CreateProviderService createProviderService;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    @Autowired
    public CreateProviderController(CreateProviderService createProviderService,
                                   SessionAuthBusiness sessionAuthBusiness) {
        this.createProviderService = createProviderService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Creates a new provider connection.
     * 
     * For OAuth providers: Returns authorization URL and state
     * For API key providers: Validates credentials and creates connection
     * 
     * @param request The provider connection request
     * @param authHeader The authorization header containing the session token
     * @return CreateProviderResponse containing connection details or OAuth URL
     */
    @PostMapping
    public ResponseEntity<CreateProviderResponse> createProvider(
            @Valid @RequestBody CreateProviderRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract user ID from the authorization token
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_INVALID_CREDENTIALS, 
                "service-provider", "Authentication token is required");
        }
        request.setUserId(userId);
        
        log.info("Creating provider connection for user: {}, provider: {}, type: {}", 
                userId, request.getProviderId(), request.getConnectionType());
        
        // Validate required fields
        if (request.getProviderId() == null || request.getProviderId().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", "providerId");
        }
        
        if (request.getConnectionType() == null || request.getConnectionType().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", "connectionType");
        }
        
        CreateProviderResponse response = createProviderService.createProvider(request);
        
        log.info("Provider connection created successfully for user: {}, provider: {}, status: {}", 
                userId, request.getProviderId(), response.getStatus());
        
        // Return 201 Created for successful provider creation
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
     * Extract user ID from the authorization token
     */
    private String extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        Optional<String> userIdOpt = sessionAuthBusiness.validateSession(token);
        return userIdOpt.orElse(null);
    }
    
} 