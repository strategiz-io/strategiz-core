package io.strategiz.api.auth.controller;

import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.data.auth.Passkey;
import io.strategiz.service.auth.PasskeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for passkey management endpoints
 */
@RestController
@RequestMapping("/auth/passkeys")
public class PasskeyController {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyController.class);

    @Autowired
    private PasskeyService passkeyService;

    /**
     * Register a new passkey
     *
     * @param userId User ID
     * @param credentialId Credential ID
     * @param publicKey Public key
     * @return Response with passkey details
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Passkey>> registerPasskey(
            @RequestParam String userId,
            @RequestParam String credentialId,
            @RequestParam String publicKey) {
        try {
            Passkey passkey = passkeyService.registerPasskey(userId, credentialId, publicKey);
            return ResponseEntity.ok(
                ApiResponse.<Passkey>success("Passkey registered successfully", passkey)
            );
        } catch (Exception e) {
            logger.error("Error registering passkey: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Passkey>error("Error registering passkey: " + e.getMessage())
            );
        }
    }

    /**
     * Get all passkeys for a user
     *
     * @param userId User ID
     * @return Response with list of passkeys
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Passkey>>> getUserPasskeys(@RequestParam String userId) {
        try {
            List<Passkey> passkeys = passkeyService.getUserPasskeys(userId);
            return ResponseEntity.ok(
                ApiResponse.<List<Passkey>>success("Passkeys retrieved successfully", passkeys)
            );
        } catch (Exception e) {
            logger.error("Error retrieving passkeys: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<List<Passkey>>error("Error retrieving passkeys: " + e.getMessage())
            );
        }
    }

    /**
     * Delete a passkey
     *
     * @param credentialId Credential ID
     * @return Response with deletion status
     */
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<ApiResponse<Boolean>> deletePasskey(@PathVariable String credentialId) {
        try {
            boolean deleted = passkeyService.deletePasskey(credentialId);
            if (deleted) {
                return ResponseEntity.ok(
                    ApiResponse.<Boolean>success("Passkey deleted successfully", true)
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Boolean>error("Passkey not found or could not be deleted", false)
                );
            }
        } catch (Exception e) {
            logger.error("Error deleting passkey: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error deleting passkey: " + e.getMessage(), false)
            );
        }
    }
}
