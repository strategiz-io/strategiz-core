package io.strategiz.api.auth.controller;

import com.google.firebase.auth.FirebaseToken;
import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.service.auth.UserAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for token verification operations
 */
@RestController
@RequestMapping("/auth/tokens/verify")
public class TokenVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(TokenVerificationController.class);

    @Autowired
    private UserAuthService userAuthService;

    /**
     * Verify a user authentication token
     *
     * @param token Authentication token
     * @return Response with token verification status and claims
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyToken(@RequestParam String token) {
        try {
            // Verify the user authentication token
            FirebaseToken decodedToken = userAuthService.verifyIdToken(token);
            
            if (decodedToken != null) {
                Map<String, Object> claims = decodedToken.getClaims();
                logger.info("Token verified successfully for user: {}", decodedToken.getUid());
                
                return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>success("Token verified successfully", claims)
                );
            } else {
                logger.warn("Invalid token provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<Map<String, Object>>error("Invalid token")
                );
            }
        } catch (Exception e) {
            logger.error("Error verifying token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Map<String, Object>>error("Error verifying token: " + e.getMessage())
            );
        }
    }
}
