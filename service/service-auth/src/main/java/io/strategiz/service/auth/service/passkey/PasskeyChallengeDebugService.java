package io.strategiz.service.auth.service.passkey;

import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.auth.repository.passkey.challenge.PasskeyChallengeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import io.strategiz.service.base.BaseService;

/**
 * Debug service to help diagnose passkey challenge issues
 */
@Service
public class PasskeyChallengeDebugService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }    
    private final PasskeyChallengeRepository challengeRepository;
    
    public PasskeyChallengeDebugService(PasskeyChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }
    
    /**
     * Debug method to list all challenges in the system
     */
    public void debugListAllChallenges() {
        log.info("=== PASSKEY CHALLENGE DEBUG ===");
        
        // Get all challenges
        List<PasskeyChallenge> allChallenges = challengeRepository.findAll();
        log.info("Total challenges in repository: {}", allChallenges.size());
        
        for (PasskeyChallenge challenge : allChallenges) {
            log.info("Challenge ID: {}", challenge.getId());
            log.info("  - Challenge: {}", challenge.getChallenge());
            log.info("  - User ID: {}", challenge.getUserId());
            log.info("  - Type: {}", challenge.getChallengeType());
            log.info("  - Created: {}", challenge.getCreatedAt());
            log.info("  - Expires: {}", challenge.getExpiresAt());
            log.info("  - Expired: {}", challenge.getExpiresAt().isBefore(Instant.now()));
            log.info("  - Used: {}", challenge.isUsed());
            log.info("---");
        }
        
        // Check for expired challenges
        List<PasskeyChallenge> expiredChallenges = challengeRepository.findByExpiresAtBefore(Instant.now());
        log.info("Expired challenges: {}", expiredChallenges.size());
        
        log.info("=== END DEBUG ===");
    }
    
    /**
     * Debug method to find challenges by partial match
     */
    public void debugFindChallengeByPartial(String partialChallenge) {
        log.info("=== SEARCHING FOR CHALLENGE CONTAINING: {} ===", partialChallenge);
        
        List<PasskeyChallenge> allChallenges = challengeRepository.findAll();
        
        for (PasskeyChallenge challenge : allChallenges) {
            if (challenge.getChallenge() != null && challenge.getChallenge().contains(partialChallenge)) {
                log.info("FOUND MATCH - Challenge: {}", challenge.getChallenge());
                log.info("  - ID: {}", challenge.getId());
                log.info("  - User: {}", challenge.getUserId());
                log.info("  - Type: {}", challenge.getChallengeType());
            }
        }
        
        log.info("=== END SEARCH ===");
    }
    
    /**
     * Create a test challenge for debugging
     */
    public PasskeyChallenge createTestChallenge(String userId, String challengeType) {
        PasskeyChallenge testChallenge = new PasskeyChallenge(
            userId,
            "TEST-CHALLENGE-" + System.currentTimeMillis(),
            Instant.now(),
            Instant.now().plusSeconds(300), // 5 minutes
            challengeType
        );
        
        PasskeyChallenge saved = challengeRepository.save(testChallenge);
        log.info("Created test challenge: {}", saved.getChallenge());
        log.info("  - ID: {}", saved.getId());
        
        return saved;
    }
}