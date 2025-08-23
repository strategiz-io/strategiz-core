package io.strategiz.service.portfolio.controller;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Debug controller to check provider integrations
 * TODO: Remove this in production
 */
@RestController
@RequestMapping("/v1/debug/providers")
public class DebugController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(DebugController.class);
    
    private final ReadProviderIntegrationRepository providerRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public DebugController(ReadProviderIntegrationRepository providerRepository,
                          UserRepository userRepository) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Check provider integrations for a user by email
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkProviderIntegrations(
            @RequestParam String email) {
        
        log.info("Checking provider integrations for email: {}", email);
        
        try {
            // Find user by email
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "User not found with email: " + email
                ));
            }
            
            UserEntity user = userOpt.get();
            String userId = user.getId();
            
            log.info("Found user with ID: {}", userId);
            
            // Get all provider integrations for this user
            List<ProviderIntegrationEntity> allProviders = providerRepository.findByUserId(userId);
            
            // Filter for Coinbase
            List<ProviderIntegrationEntity> coinbaseProviders = allProviders.stream()
                .filter(p -> "coinbase".equalsIgnoreCase(p.getProviderName()) || 
                            "coinbase".equalsIgnoreCase(p.getProviderId()))
                .collect(Collectors.toList());
            
            // Prepare response
            List<Map<String, Object>> providerDetails = new ArrayList<>();
            
            for (ProviderIntegrationEntity provider : coinbaseProviders) {
                Map<String, Object> details = new HashMap<>();
                details.put("providerId", provider.getProviderId());
                details.put("providerName", provider.getProviderName());
                details.put("providerType", provider.getProviderType());
                details.put("status", provider.getStatus());
                details.put("lastSyncAt", provider.getLastSyncAt());
                
                // Check metadata
                Map<String, Object> metadata = provider.getMetadata();
                if (metadata != null) {
                    details.put("hasAccessToken", metadata.containsKey("access_token"));
                    details.put("hasRefreshToken", metadata.containsKey("refresh_token"));
                    details.put("oauthCompleted", metadata.get("oauthCompleted"));
                    details.put("metadataKeys", metadata.keySet());
                    
                    // Show token presence but not actual values
                    if (metadata.containsKey("access_token")) {
                        String token = (String) metadata.get("access_token");
                        details.put("accessTokenLength", token != null ? token.length() : 0);
                    }
                } else {
                    details.put("metadata", "null");
                }
                
                providerDetails.add(details);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", userId,
                "email", email,
                "totalProviders", allProviders.size(),
                "coinbaseProviders", coinbaseProviders.size(),
                "providers", providerDetails
            ));
            
        } catch (Exception e) {
            log.error("Error checking provider integrations", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
    
    @Override
    protected String getModuleName() {
        return "service-portfolio";
    }
}