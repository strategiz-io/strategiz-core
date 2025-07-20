package io.strategiz.service.auth.controller.session;

import io.strategiz.service.auth.service.session.SignOutService;
import io.strategiz.service.auth.model.session.SignOutRequest;
import io.strategiz.service.auth.model.session.SignOutResponse;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Controller for user sign-out operations.
 * Handles user logout and session cleanup.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/auth/signout")
public class SignOutController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger log = LoggerFactory.getLogger(SignOutController.class);
    
    private final SignOutService signOutService;
    
    public SignOutController(SignOutService signOutService) {
        this.signOutService = signOutService;
    }
    
    /**
     * Sign out a user and clean up their session
     * 
     * @param request Sign out request containing user session information
     * @return Clean sign out response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping
    public ResponseEntity<SignOutResponse> signOut(@Valid @RequestBody SignOutRequest request) {
        log.info("Processing sign out for user: {}", request.userId());
        
        // Sign out user - let exceptions bubble up
        SignOutResponse response = signOutService.signOut(
            request.userId(),
            request.sessionId(),
            request.deviceId(),
            request.revokeAllSessions()
        );
        
        // Return clean response using BaseController method
        return createCleanResponse(response);
    }
}
