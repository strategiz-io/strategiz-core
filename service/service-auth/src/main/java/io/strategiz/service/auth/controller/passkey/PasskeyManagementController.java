package io.strategiz.service.auth.controller.passkey;

import io.strategiz.service.auth.service.passkey.PasskeyManagementService;
import io.strategiz.service.auth.service.passkey.PasskeyManagementService.PasskeyDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controller for passkey management operations.
 * Handles listing and deleting passkeys.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/auth/passkey/manage")
public class PasskeyManagementController {

    private static final Logger log = LoggerFactory.getLogger(PasskeyManagementController.class);
    
    private final PasskeyManagementService passkeyManagementService;
    
    public PasskeyManagementController(PasskeyManagementService passkeyManagementService) {
        this.passkeyManagementService = passkeyManagementService;
    }
    
    /**
     * List all passkeys for a user
     * 
     * @param userId The user ID to list passkeys for
     * @return Clean list of passkeys - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/list/{userId}")
    public ResponseEntity<List<PasskeyDetails>> listPasskeys(@PathVariable String userId) {
        log.info("Listing passkeys for user: {}", userId);
        
        // List passkeys - let exceptions bubble up
        List<PasskeyDetails> passkeys = passkeyManagementService.getPasskeysForUser(userId);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(passkeys);
    }
    
    /**
     * Delete a passkey
     * 
     * @param request Request containing userId and credentialId
     * @return Clean delete response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deletePasskey(@Valid @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String credentialId = request.get("credentialId");
        
        log.info("Deleting passkey {} for user: {}", credentialId, userId);
        
        // Delete passkey - let exceptions bubble up
        boolean deleted = passkeyManagementService.deletePasskey(userId, credentialId);
        
        Map<String, Object> result = Map.of(
            "deleted", deleted,
            "credentialId", credentialId,
            "message", deleted ? "Passkey deleted successfully" : "Passkey not found or could not be deleted"
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get passkey statistics for a user
     * 
     * @param userId The user ID to get statistics for
     * @return Clean statistics response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getPasskeyStats(@PathVariable String userId) {
        log.info("Getting passkey statistics for user: {}", userId);
        
        // Get passkey count and basic stats
        List<PasskeyDetails> passkeys = passkeyManagementService.getPasskeysForUser(userId);
        
        Map<String, Object> stats = Map.of(
            "count", passkeys.size(),
            "userId", userId,
            "hasPasskeys", !passkeys.isEmpty()
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(stats);
    }
}
