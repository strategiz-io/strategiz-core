package io.strategiz.service.auth.service.passkey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.auth.repository.passkey.challenge.PasskeyChallengeRepository;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import io.strategiz.service.base.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing passkey challenges for WebAuthn registration and authentication flows
 */
@Service
public class PasskeyChallengeService extends BaseService {
    
    @Override
    protected String getModuleName() {
        return "service-auth";
    }
    
    private static final Logger log = LoggerFactory.getLogger(PasskeyChallengeService.class);
    
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${passkey.challengeTimeoutMs:60000}")
    private long challengeTimeoutMs;
    
    public PasskeyChallengeService(PasskeyChallengeRepository passkeyChallengeRepository) {
        this.passkeyChallengeRepository = passkeyChallengeRepository;
        this.objectMapper = new ObjectMapper();
        
        // Ensure we're using real passkey challenge generation, not mock data
        ensureRealApiData("PasskeyChallengeService");
    }
    
    /**
     * Create a new WebAuthn challenge
     *
     * @param userId UserEntity ID (optional for authentication challenges)
     * @param type Challenge type (authentication or registration)
     * @return The challenge string
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public String createChallenge(String userId, PasskeyChallengeType type) {
        try {
            // Generate a random challenge using URL-safe Base64 encoded UUID
            String challenge = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(UUID.randomUUID().toString().getBytes());
                    
            // Create a new challenge entity
            Instant now = Instant.now();
            
            // Use the constructor instead of setter methods
            PasskeyChallenge challengeEntity = new PasskeyChallenge(
                userId,
                challenge,
                now,
                now.plusMillis(challengeTimeoutMs),
                type.name()
            );
            
            // Save the challenge (repository will handle ID generation and audit fields)
            // The repository implementation will use the userId from the entity
            log.debug("About to save challenge - entity before save: ID={}, userId={}, challenge={}, type={}", 
                challengeEntity.getId(), challengeEntity.getUserId(), challengeEntity.getChallenge(), challengeEntity.getType());
            
            challengeEntity = passkeyChallengeRepository.save(challengeEntity);
            
            log.debug("Challenge saved successfully - entity after save: ID={}, userId={}, challenge={}, type={}, isActive={}, hasAudit={}", 
                challengeEntity.getId(), challengeEntity.getUserId(), challengeEntity.getChallenge(), challengeEntity.getType(),
                challengeEntity.isActive(), challengeEntity._hasAudit());
            
            // Add a small delay to ensure Firestore consistency
            try {
                Thread.sleep(100); // 100ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Try multiple verification approaches
            log.debug("=== VERIFICATION PHASE ===");
            
            // 1. Direct challenge lookup
            Optional<PasskeyChallenge> savedChallenge = passkeyChallengeRepository.findByChallenge(challenge);
            log.debug("1. Direct challenge lookup - found: {}", savedChallenge.isPresent());
            if (savedChallenge.isPresent()) {
                PasskeyChallenge found = savedChallenge.get();
                log.debug("   Found challenge details - ID: {}, userId: {}, type: {}, challenge: {}", 
                    found.getId(), found.getUserId(), found.getChallengeType(), found.getChallenge());
            }
            
            // 2. User challenges lookup
            List<PasskeyChallenge> userChallenges = passkeyChallengeRepository.findByUserId(userId);
            log.debug("2. User challenges lookup - total for user {}: {}", userId, userChallenges.size());
            for (PasskeyChallenge c : userChallenges) {
                log.debug("   User challenge: ID={}, challenge={}, type={}, matches={}", 
                    c.getId(), c.getChallenge(), c.getChallengeType(), challenge.equals(c.getChallenge()));
            }
            
            // 3. All challenges lookup
            List<PasskeyChallenge> allChallenges = passkeyChallengeRepository.findAll();
            log.debug("3. All challenges lookup - total in database: {}", allChallenges.size());
            for (PasskeyChallenge c : allChallenges) {
                if (challenge.equals(c.getChallenge())) {
                    log.debug("   FOUND MATCHING CHALLENGE: ID={}, userId={}, type={}", 
                        c.getId(), c.getUserId(), c.getChallengeType());
                }
            }
            
            // 4. Exists check
            boolean exists = passkeyChallengeRepository.existsByChallenge(challenge);
            log.debug("4. Exists check - challenge exists: {}", exists);
            
            // 5. Raw Firestore debug - try to access the collection directly
            try {
                log.debug("5. Direct Firestore access attempt...");
                // Try to get the collection reference and query directly
                if (passkeyChallengeRepository instanceof io.strategiz.data.auth.repository.passkey.challenge.PasskeyChallengeRepositoryImpl) {
                    log.debug("Repository is correct type, attempting direct debug");
                    ((io.strategiz.data.auth.repository.passkey.challenge.PasskeyChallengeRepositoryImpl) passkeyChallengeRepository).debugFirestoreContents();
                } else {
                    log.debug("Repository type: {}", passkeyChallengeRepository.getClass().getName());
                }
            } catch (Exception e) {
                log.debug("Error in direct Firestore access", e);
            }
            
            log.debug("=== END VERIFICATION ===");
            
            return challenge;
        } catch (Exception e) {
            throwModuleException(ServiceAuthErrorDetails.PASSKEY_REGISTRATION_FAILED, e, e.getMessage());
            return null; // This line is unreachable but required for compilation
        }
    }
    
    /**
     * Verify and validate a challenge
     *
     * @param challenge The challenge string to verify
     * @param userId UserEntity ID (optional for authentication challenges)
     * @param type Challenge type (authentication or registration)
     * @return True if the challenge is valid, false otherwise
     */
    @Transactional
    public boolean verifyChallenge(String challenge, String userId, PasskeyChallengeType type) {
        Instant now = Instant.now();
        
        log.debug("Verifying challenge: {} for user: {} with type: {}", challenge, userId, type);
        
        // Find challenge matching the challenge string
        Optional<PasskeyChallenge> foundChallenge = passkeyChallengeRepository.findByChallenge(challenge);
        log.debug("Challenge found: {}", foundChallenge.isPresent());
        
        Optional<PasskeyChallenge> challengeOpt = foundChallenge
                .filter(c -> type.name().equals(c.getChallengeType()))
                // For authentication challenges, userId may be null initially
                .filter(c -> type == PasskeyChallengeType.AUTHENTICATION 
                        ? true 
                        : (userId != null && userId.equals(c.getUserId())))
                .filter(c -> c.getExpiresAt().isAfter(now));
                
        if (challengeOpt.isPresent()) {
            log.debug("Challenge verified successfully for user: {}", userId);
            // Challenge is valid, delete it to prevent replay attacks
            passkeyChallengeRepository.delete(challengeOpt.get());
            
            // Also clean up any expired challenges of this type
            passkeyChallengeRepository.findByChallengeType(type.name())
                    .stream()
                    .filter(c -> c.getExpiresAt().isBefore(now))
                    .forEach(passkeyChallengeRepository::delete);
                    
            return true;
        }
        
        log.debug("Challenge verification failed. No matching valid challenge found.");
        return false;
    }
    
    /**
     * Extract the challenge string from client data JSON
     *
     * @param clientDataJSON Base64URL encoded client data JSON
     * @return Extracted challenge string
     */
    public String extractChallengeFromClientData(String clientDataJSON) {
        try {
            // Decode the Base64URL encoded JSON
            byte[] clientDataBytes = Base64.getUrlDecoder().decode(clientDataJSON);
            String clientData = new String(clientDataBytes);
            
            log.debug("Decoded clientDataJSON: {}", clientData);
            
            // Parse the JSON
            JsonNode clientDataNode = objectMapper.readTree(clientData);
            String challenge = clientDataNode.path("challenge").asText("");
            
            if (challenge.isEmpty()) {
                log.warn("No challenge found in client data JSON");
                return "";
            }
            
            log.debug("Extracted challenge from clientDataJSON: {}", challenge);
            
            return challenge;
        } catch (Exception e) {
            log.error("Error extracting challenge from client data. clientDataJSON: {}", clientDataJSON, e);
            return "";
        }
    }
    
    /**
     * Find the most recent challenge for a user
     * @param userId UserEntity ID
     * @param type Challenge type
     * @return Optional containing the challenge if found
     */
    public Optional<String> findMostRecentChallengeForUser(String userId, PasskeyChallengeType type) {
        if (userId == null) {
            return Optional.empty();
        }
        
        Instant now = Instant.now();
        
        return passkeyChallengeRepository.findByUserId(userId)
                .stream()
                .filter(c -> type.name().equals(c.getChallengeType()))
                .filter(c -> c.getExpiresAt().isAfter(now))
                .sorted(Comparator.comparing(PasskeyChallenge::getCreatedAt).reversed())
                .map(PasskeyChallenge::getChallenge)
                .findFirst();
    }
    
    /**
     * Clean up expired challenges
     * <p>
     * This method deletes all challenges that have expired.
     * It can be called explicitly or triggered by a scheduled task.
     * 
     * @return Number of challenges deleted
     */
    @Transactional
    public int cleanExpiredChallenges() {
        log.debug("Cleaning up expired challenges");
        Instant now = Instant.now();
        
        List<PasskeyChallenge> expiredChallenges = passkeyChallengeRepository.findByExpiresAtBefore(now);
                
        int count = expiredChallenges.size();
        if (count > 0) {
            log.info("Deleting {} expired challenges", count);
            passkeyChallengeRepository.deleteAll(expiredChallenges);
        }
        
        return count;
    }
    
    /**
     * Scheduled task to clean up expired challenges
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void scheduledCleanup() {
        int deleted = cleanExpiredChallenges();
        log.debug("Scheduled cleanup deleted {} expired challenges", deleted);
    }
}
