package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.response.ReadProviderResponse;
import io.strategiz.service.provider.service.ReadProviderService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.provider.exception.ProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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
    
    @Autowired
    public ReadProviderController(ReadProviderService readProviderService) {
        this.readProviderService = readProviderService;
    }
    
    /**
     * Get all providers for a user.
     * 
     * @param principal The authenticated user principal
     * @return List of providers with their status and data
     */
    @GetMapping
    public ResponseEntity<ReadProviderResponse> getProviders(Principal principal) {
        
        // Extract user ID from authentication principal
        String userId = principal != null ? principal.getName() : "anonymous";
        
        log.info("Retrieving providers for user: {}", userId);
        
        try {
            ReadProviderResponse response = readProviderService.getProviders(userId);
            
            log.info("Retrieved provider data for user: {}, status: {}", userId, response.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving providers for user: {}", userId, e);
            
            throw new StrategizException(ProviderErrorDetails.PROVIDER_SERVICE_UNAVAILABLE, "service-provider", 
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
            
            throw new StrategizException(ProviderErrorDetails.PROVIDER_NOT_FOUND, "service-provider", 
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
            
            throw new StrategizException(ProviderErrorDetails.PROVIDER_NOT_FOUND, "service-provider", 
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
            
            throw new StrategizException(ProviderErrorDetails.PROVIDER_DATA_UNAVAILABLE, "service-provider", 
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