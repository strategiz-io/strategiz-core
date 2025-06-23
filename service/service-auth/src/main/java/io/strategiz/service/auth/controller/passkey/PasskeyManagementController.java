package io.strategiz.service.auth.controller.passkey;

import io.strategiz.service.auth.model.passkey.Passkey;
import io.strategiz.service.auth.service.passkey.PasskeyManagementService;
import io.strategiz.service.auth.service.passkey.PasskeyManagementService.PasskeyDetails;
import io.strategiz.service.auth.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for passkey management operations (listing, deletion)
 */
@RestController
@RequestMapping("/auth/passkey/management")
public class PasskeyManagementController {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyManagementController.class);

    @Autowired
    private PasskeyManagementService managementService;

    /**
     * Get all passkeys for a user
     *
     * @param userId User ID
     * @return Response with list of passkeys
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PasskeyDto>>> getUserPasskeys(@RequestParam String userId) {
        try {
            logger.info("Retrieving passkeys for user: {}", userId);
            List<PasskeyDetails> servicePasskeys = managementService.getPasskeysForUser(userId);
            List<PasskeyDto> apiPasskeys = convertToPasskeyDtos(servicePasskeys);
            
            logger.debug("Retrieved {} passkeys for user {}", apiPasskeys.size(), userId);
            return ResponseEntity.ok(
                ApiResponse.<List<PasskeyDto>>success("Passkeys retrieved successfully", apiPasskeys)
            );
        } catch (Exception e) {
            logger.error("Error retrieving passkeys: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<List<PasskeyDto>>error("Error retrieving passkeys: " + e.getMessage())
            );
        }
    }
    
    /**
     * Converts service layer passkey models to API response DTOs
     * 
     * @param servicePasskeys List of service layer passkeys
     * @return List of passkey DTOs for the API
     */
    private List<PasskeyDto> convertToPasskeyDtos(List<PasskeyDetails> servicePasskeys) {
        if (servicePasskeys == null) {
            return new ArrayList<>();
        }
        
        return servicePasskeys.stream()
            .map(sp -> new PasskeyDto(
                sp.id(),                  // Credential ID
                null,                     // User ID not provided in simplified PasskeyDetails
                sp.name(),                // Authenticator name
                sp.registeredAt(),        // Registration time 
                sp.lastUsedAt(),          // Last used time
                null                      // AAGUID not provided in simplified PasskeyDetails
            ))
            .collect(Collectors.toList());
    }

    /**
     * Delete a passkey
     *
     * @param credentialId Credential ID
     * @return Response with deletion status
     */
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<ApiResponse<Boolean>> deletePasskey(
            @PathVariable String credentialId, 
            @RequestParam String userId) {
        try {
            logger.info("Attempting to delete passkey {} for user {}", credentialId, userId);
            
            boolean deleted = managementService.deletePasskey(userId, credentialId);
            
            if (deleted) {
                logger.info("Successfully deleted passkey {} for user {}", credentialId, userId);
                return ResponseEntity.ok(
                    ApiResponse.<Boolean>success("Passkey deleted successfully", true)
                );
            } else {
                logger.warn("Failed to delete passkey {} for user {}: not found or not owned by user", 
                        credentialId, userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Boolean>error("Passkey not found or not owned by user")
                );
            }
        } catch (Exception e) {
            logger.error("Error deleting passkey: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error deleting passkey: " + e.getMessage())
            );
        }
    }
    
    /**
     * Data Transfer Object for Passkey API responses
     * This is a simplified version of the service layer Passkey class
     * It contains only the fields needed for client display
     */
    public record PasskeyDto(
        String credentialId,
        String userId,
        String authenticatorName,
        Instant registrationTime,
        Instant lastUsedTime,
        String aaguid
    ) {}
}
