package io.strategiz.service.auth.service.passkey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.auth.repository.passkey.challenge.PasskeyChallengeRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.data.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
public class PasskeyChallengeService {
    
    private static final Logger log = LoggerFactory.getLogger(PasskeyChallengeService.class);
    
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${passkey.challengeTimeoutMs:60000}")
    private long challengeTimeoutMs;
    
    public PasskeyChallengeService(PasskeyChallengeRepository passkeyChallengeRepository) {
        this.passkeyChallengeRepository = passkeyChallengeRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Create a new WebAuthn challenge
     *
     * @param userId User ID (optional for authentication challenges)
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
            
            // Let Hibernate generate the ID through the @GeneratedValue annotation
            // Save the challenge
            passkeyChallengeRepository.saveAndFlush(challengeEntity);
            log.debug("Created new {} challenge for user {}", type, userId);
            
            return challenge;
        } catch (Exception e) {
            throw new StrategizException(AuthErrors.PASSKEY_REGISTRATION_FAILED, e.getMessage());
        }
    }
    
    /**
     * Verify and validate a challenge
     *
     * @param challenge The challenge string to verify
     * @param userId User ID (optional for authentication challenges)
     * @param type Challenge type (authentication or registration)
     * @return True if the challenge is valid, false otherwise
     */
    @Transactional
    public boolean verifyChallenge(String challenge, String userId, PasskeyChallengeType type) {
        Instant now = Instant.now();
        
        // Find challenges matching the criteria
        Optional<PasskeyChallenge> challengeOpt = passkeyChallengeRepository.findByChallenge(challenge)
                .stream()
                .filter(c -> type.name().equals(c.getChallengeType()))
                // For authentication challenges, userId may be null initially
                .filter(c -> type == PasskeyChallengeType.AUTHENTICATION 
                        ? true 
                        : (userId != null && userId.equals(c.getUserId())))
                .filter(c -> c.getExpiresAt().isAfter(now))
                .findFirst();
                
        if (challengeOpt.isPresent()) {
            // Challenge is valid, delete it to prevent replay attacks
            passkeyChallengeRepository.delete(challengeOpt.get());
            
            // Also clean up any expired challenges of this type
            passkeyChallengeRepository.findByChallengeType(type.name())
                    .stream()
                    .filter(c -> c.getExpiresAt().isBefore(now))
                    .forEach(passkeyChallengeRepository::delete);
                    
            return true;
        }
        
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
            
            // Parse the JSON
            JsonNode clientDataNode = objectMapper.readTree(clientData);
            String challenge = clientDataNode.path("challenge").asText("");
            
            if (challenge.isEmpty()) {
                log.warn("No challenge found in client data JSON");
                return "";
            }
            
            return challenge;
        } catch (IOException e) {
            log.error("Error extracting challenge from client data", e);
            return "";
        }
    }
    
    /**
     * Find the most recent challenge for a user
     * @param userId User ID
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
        
        List<PasskeyChallenge> expiredChallenges = passkeyChallengeRepository.findAll().stream()
                .filter(c -> c.getExpiresAt().isBefore(now))
                .toList();
                
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
