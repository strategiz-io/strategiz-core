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
import java.security.Principal;
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
     * @param principal The authenticated user principal from session
     * @return CreateProviderResponse containing connection details or OAuth URL
     */
    @PostMapping
    public ResponseEntity<CreateProviderResponse> createProvider(
            @Valid @RequestBody CreateProviderRequest request,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        log.info("CreateProvider: Principal userId: {}, AuthHeader present: {}",
                userId, authHeader != null);

        // If session auth failed, try token-based auth as fallback (for signup flow)
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    log.info("Provider creation authenticated via Bearer token for user: {}", userId);
                } else {
                    log.warn("Invalid Bearer token provided for provider creation");
                }
            } catch (Exception e) {
                log.warn("Error validating Bearer token for provider creation: {}", e.getMessage());
            }
        }

        if (userId == null) {
            log.error("No valid authentication session or token for provider creation");
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_INVALID_CREDENTIALS,
                "service-provider", "Authentication required to connect provider");
        }
        request.setUserId(userId);
        
        log.info("Creating provider connection for user: {}, provider: {}, type: {}", 
                userId, request.getProviderId(), request.getConnectionType());
        
        // Debug log to check credentials
        if (request.getCredentials() != null) {
            log.debug("Received credentials map with {} keys: {}", 
                    request.getCredentials().size(), 
                    request.getCredentials().keySet());
        } else {
            log.debug("No credentials provided in request");
        }
        
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
     * Fetches connected providers for the authenticated user.
     *
     * @param principal The authenticated user principal from session
     * @return Response containing the list of connected providers
     */
    @GetMapping("/connected")
    public ResponseEntity<?> getConnectedProviders(Principal principal) {

        String userId = principal != null ? principal.getName() : null;
        if (userId == null) {
            log.error("No valid authentication session for fetching providers");
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_INVALID_CREDENTIALS,
                "service-provider", "Authentication required to fetch providers");
        }
        
        log.info("Fetching connected providers for user: {}", userId);
        
        try {
            var connectedProviders = createProviderService.getConnectedProviders(userId);
            return ResponseEntity.ok(connectedProviders);
        } catch (Exception e) {
            log.error("Error fetching connected providers for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_CONNECTION_FAILED, 
                "service-provider", "Failed to fetch connected providers");
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
    
} 