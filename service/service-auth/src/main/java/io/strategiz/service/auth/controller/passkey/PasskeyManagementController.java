package io.strategiz.service.auth.controller.passkey;

import io.strategiz.service.auth.service.passkey.PasskeyManagementService;
import io.strategiz.service.auth.service.passkey.PasskeyManagementService.PasskeyDetails;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controller for passkey management operations using resource-based REST endpoints
 * 
 * This controller handles passkey CRUD operations following REST best practices
 * with proper resource naming and HTTP verbs.
 * 
 * Endpoints:
 * - GET /auth/passkeys - List user's passkeys
 * - DELETE /auth/passkeys/{id} - Delete specific passkey
 * - GET /auth/passkeys/stats - Get passkey statistics
 * 
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/auth/passkeys")
public class PasskeyManagementController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger log = LoggerFactory.getLogger(PasskeyManagementController.class);
    
    private final PasskeyManagementService passkeyManagementService;
    
    public PasskeyManagementController(PasskeyManagementService passkeyManagementService) {
        this.passkeyManagementService = passkeyManagementService;
    }
    
    /**
     * List all passkeys for a user
     * 
     * GET /auth/passkeys?userId={userId}
     * 
     * @param userId The user ID to list passkeys for
     * @return Clean list of passkeys - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping
    public ResponseEntity<List<PasskeyDetails>> listPasskeys(@RequestParam String userId) {
        logRequest("listPasskeys", userId);
        
        // List passkeys - let exceptions bubble up
        List<PasskeyDetails> passkeys = passkeyManagementService.getPasskeysForUser(userId);
        
        logRequestSuccess("listPasskeys", userId, passkeys);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(passkeys);
    }
    
    /**
     * Delete a specific passkey
     * 
     * DELETE /auth/passkeys/{id}?userId={userId}
     * 
     * @param credentialId The credential ID to delete
     * @param userId The user ID for authorization
     * @return Clean delete response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<Map<String, Object>> deletePasskey(
            @PathVariable String credentialId,
            @RequestParam String userId) {
        
        logRequest("deletePasskey", userId);
        
        // Delete passkey - let exceptions bubble up
        boolean deleted = passkeyManagementService.deletePasskey(userId, credentialId);
        
        Map<String, Object> result = Map.of(
            "deleted", deleted,
            "credentialId", credentialId,
            "message", deleted ? "Passkey deleted successfully" : "Passkey not found or could not be deleted"
        );
        
        logRequestSuccess("deletePasskey", userId, result);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(result);
    }
    
    /**
     * Get passkey statistics for a user
     * 
     * GET /auth/passkeys/stats?userId={userId}
     * 
     * @param userId The user ID to get statistics for
     * @return Clean statistics response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPasskeyStats(@RequestParam String userId) {
        logRequest("getPasskeyStats", userId);
        
        // Get passkey count and basic stats
        List<PasskeyDetails> passkeys = passkeyManagementService.getPasskeysForUser(userId);
        
        Map<String, Object> stats = Map.of(
            "count", passkeys.size(),
            "userId", userId,
            "hasPasskeys", !passkeys.isEmpty()
        );
        
        logRequestSuccess("getPasskeyStats", userId, stats);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(stats);
    }
}
