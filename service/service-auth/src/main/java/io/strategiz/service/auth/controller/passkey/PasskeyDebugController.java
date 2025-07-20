package io.strategiz.service.auth.controller.passkey;

import io.strategiz.service.auth.service.passkey.PasskeyChallengeDebugService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Debug controller for passkey challenge issues
 */
@RestController
@RequestMapping("/v1/auth/passkeys/debug")
public class PasskeyDebugController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger log = LoggerFactory.getLogger(PasskeyDebugController.class);
    
    @Autowired
    private PasskeyChallengeDebugService debugService;

    /**
     * List all challenges for debugging
     */
    @GetMapping("/challenges")
    public ResponseEntity<Map<String, Object>> listAllChallenges() {
        log.info("DEBUG: Listing all passkey challenges");
        
        debugService.debugListAllChallenges();
        
        return createCleanResponse(Map.of(
            "message", "Check server logs for challenge details",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Search for challenges containing partial string
     */
    @GetMapping("/challenges/search")
    public ResponseEntity<Map<String, Object>> searchChallenge(@RequestParam String partial) {
        log.info("DEBUG: Searching for challenges containing: {}", partial);
        
        debugService.debugFindChallengeByPartial(partial);
        
        return createCleanResponse(Map.of(
            "message", "Check server logs for search results",
            "searched", partial,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Create a test challenge
     */
    @PostMapping("/challenges/test")
    public ResponseEntity<Map<String, Object>> createTestChallenge(
            @RequestParam String userId,
            @RequestParam(defaultValue = "REGISTRATION") String type) {
        
        log.info("DEBUG: Creating test challenge for user: {} with type: {}", userId, type);
        
        var challenge = debugService.createTestChallenge(userId, type);
        
        return createCleanResponse(Map.of(
            "challengeId", challenge.getId(),
            "challenge", challenge.getChallenge(),
            "userId", challenge.getUserId(),
            "type", challenge.getChallengeType(),
            "expiresAt", challenge.getExpiresAt().toString()
        ));
    }
}