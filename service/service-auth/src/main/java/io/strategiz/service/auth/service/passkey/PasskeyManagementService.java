package io.strategiz.service.auth.service.passkey;

import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.service.auth.converter.PasskeyCredentialConverter;
import io.strategiz.service.base.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing passkey credentials
 * <p>
 * This service provides functionality to list, retrieve, and delete passkey
 * credentials for users. It serves as a management layer between the API controllers
 * and the underlying data repositories.
 * <p>
 * Usage examples:
 * <pre>
 * // List all passkeys for a user
 * List<PasskeyDetails> userPasskeys = passkeyManagementService.getPasskeysForUser(userId);
 * 
 * // Delete a specific passkey
 * boolean deleted = passkeyManagementService.deletePasskey(userId, credentialId);
 * </pre>
 */
@Service
public class PasskeyManagementService extends BaseService {
    
    @Override
    protected String getModuleName() {
        return "service-auth";
    }
    private static final Logger logger = LoggerFactory.getLogger(PasskeyManagementService.class);
    
    private final PasskeyCredentialRepository credentialRepository;
    private final PasskeyCredentialConverter credentialConverter;
    
    public PasskeyManagementService(PasskeyCredentialRepository credentialRepository, 
                                  PasskeyCredentialConverter credentialConverter) {
        this.credentialRepository = credentialRepository;
        this.credentialConverter = credentialConverter;
        
        // Ensure we're using real passkey management, not mock data
        ensureRealApiData("PasskeyManagementService");
    }
    
    /**
     * Retrieve all passkeys registered to a specific user
     * <p>
     * This method fetches all passkey credentials associated with the provided user ID
     * and converts them to a simplified representation suitable for API consumption.
     * The passkeys are returned with basic information about each credential including
     * its ID, name, registration time, and last used time.
     * 
     * @param userId UserEntity ID to retrieve passkeys for
     * @return List of passkey details for the user (empty list if none found)
     */
    public List<PasskeyDetails> getPasskeysForUser(String userId) {
        List<PasskeyCredential> credentials = credentialConverter.toDomainModels(
            credentialRepository.findByUserId(userId)
        );
        return credentials.stream()
            .map(this::convertToDetails)
            // Sort by registeredAt in ascending order (oldest first)
            // Using Comparator.nullsLast to handle potential nulls gracefully
            .sorted(Comparator.comparing(PasskeyDetails::registeredAt, 
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }
    
    /**
     * Delete a passkey credential for a user
     * <p>
     * This method allows deletion of a specific passkey credential identified by its ID.
     * For security, the method verifies that the credential belongs to the specified user 
     * before deletion to prevent unauthorized removal of credentials.
     * <p>
     * The deletion process includes these verification steps:
     * <ol>
     *   <li>Check if the credential exists</li>
     *   <li>Verify the credential belongs to the specified user</li>
     *   <li>Delete the credential if both checks pass</li>
     * </ol>
     * 
     * @param userId UserEntity ID of the credential owner
     * @param credentialId Credential ID to delete
     * @return true if credential was found and deleted, false if not found or not owned by user
     */
    @Transactional
    public boolean deletePasskey(String userId, String credentialId) {
        logger.debug("Attempting to delete passkey credential: {} for user: {}", credentialId, userId);
        
        Optional<PasskeyCredential> credentialOpt = credentialConverter.toDomainModel(
            credentialRepository.findByCredentialId(credentialId)
        );
        
        if (credentialOpt.isEmpty()) {
            logger.warn("Passkey deletion failed: Credential not found: {}", credentialId);
            return false;
        }
        
        PasskeyCredential credential = credentialOpt.get();
        
        // Verify user owns the credential
        if (!credential.getUserId().equals(userId)) {
            logger.warn("Passkey deletion failed: UserEntity {} attempted to delete credential {} belonging to user {}", 
                    userId, credentialId, credential.getUserId());
            return false;
        }
        
        logger.info("Deleting passkey credential: {} for user: {}", credentialId, userId);
        credentialRepository.delete(credentialConverter.toEntity(credential));
        return true;
    }
    
    /**
     * Convert a passkey credential to passkey details
     */
    private PasskeyDetails convertToDetails(PasskeyCredential credential) {
        return new PasskeyDetails(
            credential.getCredentialId(),
            credential.getAuthenticatorName(),
            credential.getRegistrationTime(),
            credential.getLastUsedTime()
        );
    }
    
    /**
     * Passkey details record
     * <p>
     * This record represents the simplified view of a passkey credential,
     * containing only the information needed for display and management in user interfaces.
     * It excludes sensitive information like the public key and raw credential data.
     * 
     * @param id The unique identifier of the passkey credential
     * @param name A human-readable name for the credential (typically device name)
     * @param registeredAt Timestamp when the credential was first registered
     * @param lastUsedAt Timestamp when the credential was last used for authentication
     */
    public record PasskeyDetails(
            String id,
            String name,
            Instant registeredAt,
            Instant lastUsedAt) {}
}
