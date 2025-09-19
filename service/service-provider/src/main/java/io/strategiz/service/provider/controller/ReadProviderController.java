package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.response.ReadProviderResponse;
import io.strategiz.service.provider.model.response.ProvidersListResponse;
import io.strategiz.service.provider.service.ReadProviderService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

/**
 * Controller for reading provider connections and data.
 * Handles retrieving provider status, balances, transactions, and connection information.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers")
public class ReadProviderController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.PROVIDER_MODULE;
    }
    
    private final ReadProviderService readProviderService;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    @Autowired
    public ReadProviderController(ReadProviderService readProviderService,
                                  SessionAuthBusiness sessionAuthBusiness) {
        this.readProviderService = readProviderService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Get all providers for a user.
     * 
     * @param principal The authenticated user principal
     * @return List of providers with their status and data
     */
    @GetMapping
    public ResponseEntity<ProvidersListResponse> getProviders(Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Get user ID from session
        String userId = null;
        log.debug("ReadProviderController: Auth header present: {}, Principal present: {}", 
            authHeader != null, principal != null);
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("ReadProviderController: Validating session with token (first 10 chars): {}", 
                token.length() > 10 ? token.substring(0, 10) + "..." : token);
            
            Optional<String> sessionUserId = sessionAuthBusiness.validateSession(token);
            if (sessionUserId.isPresent()) {
                userId = sessionUserId.get();
                log.info("ReadProviderController: Got userId from session: {}", userId);
            } else {
                log.warn("ReadProviderController: Session validation returned empty - token might be invalid");
            }
        }
        
        // Fallback to principal if session doesn't provide userId
        if (userId == null && principal != null) {
            userId = principal.getName();
            log.info("ReadProviderController: Using userId from principal: {}", userId);
        }
        
        if (userId == null) {
            log.error("ReadProviderController: No user ID available from authentication");
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                "userId", "null", "User must be authenticated");
        }
        
        log.info("Retrieving providers for user: {}", userId);
        
        try {
            ProvidersListResponse response = readProviderService.getProvidersList(userId);
            
            log.info("Retrieved {} providers for user: {}", 
                    response.getTotalCount(), userId);
            
            // Log provider details
            if (response.getProviders() != null && !response.getProviders().isEmpty()) {
                for (ReadProviderResponse provider : response.getProviders()) {
                    log.info("Provider in response: providerId={}, status={}, providerName={}", 
                        provider.getProviderId(), provider.getStatus(), provider.getProviderName());
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving providers for user: {}", userId, e);
            
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_SERVICE_UNAVAILABLE, "service-provider", 
                userId, e.getMessage());
        }
    }
    
    /**
     * Get a specific provider by ID.
     * 
     * @param providerId The provider ID to retrieve
     * @param principal The authenticated user principal
     * @return Provider details
     */
    @GetMapping("/{providerId}")
    public ResponseEntity<ReadProviderResponse> getProvider(
            @PathVariable String providerId,
            Principal principal) {
        
        // Extract user ID from authentication principal
        String userId = principal != null ? principal.getName() : "anonymous";
        
        log.info("Retrieving provider {} for user: {}", providerId, userId);
        
        try {
            ReadProviderResponse response = readProviderService.getProvider(userId, providerId);
            
            return ResponseEntity.ok(response);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving provider {} for user: {}", providerId, userId, e);
            
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_NOT_FOUND, "service-provider", 
                userId, providerId);
        }
    }
    
    /**
     * Get provider status (lightweight check).
     * 
     * @param providerId The provider ID to check
     * @param principal The authenticated user principal
     * @return Provider status information
     */
    @GetMapping("/{providerId}/status")
    public ResponseEntity<ReadProviderResponse> getProviderStatus(
            @PathVariable String providerId,
            Principal principal) {
        
        // Extract user ID from authentication principal
        String userId = principal != null ? principal.getName() : "anonymous";
        
        log.info("Checking status for provider {} for user: {}", providerId, userId);
        
        try {
            ReadProviderResponse response = readProviderService.getProviderStatus(userId, providerId);
            
            return ResponseEntity.ok(response);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking status for provider {} for user: {}", providerId, userId, e);
            
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_NOT_FOUND, "service-provider", 
                userId, providerId);
        }
    }
    
    /**
     * Get provider data (balances, transactions, etc.).
     * 
     * @param providerId The provider ID to get data for
     * @param dataType The type of data to retrieve (balances, transactions, etc.)
     * @param principal The authenticated user principal
     * @return Provider data
     */
    @GetMapping("/{providerId}/data")
    public ResponseEntity<ReadProviderResponse> getProviderData(
            @PathVariable String providerId,
            @RequestParam(required = false, defaultValue = "balances") String dataType,
            Principal principal) {
        
        // Extract user ID from authentication principal
        String userId = principal != null ? principal.getName() : "anonymous";
        
        log.info("Retrieving {} data for provider {} for user: {}", dataType, providerId, userId);
        
        try {
            ReadProviderResponse response = readProviderService.getProviderData(userId, providerId, dataType);
            
            return ResponseEntity.ok(response);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving {} data for provider {} for user: {}", dataType, providerId, userId, e);
            
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_DATA_UNAVAILABLE, "service-provider", 
                userId, providerId, dataType);
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
}