package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.DeleteProviderRequest;
import io.strategiz.service.provider.model.response.DeleteProviderResponse;
import io.strategiz.service.provider.service.DeleteProviderService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller for deleting provider connections and data.
 * Handles HTTP requests for disconnecting providers and cleaning up associated data.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers")
public class DeleteProviderController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.PROVIDER_MODULE;
    }

    @Autowired
    private DeleteProviderService deleteProviderService;

    @Autowired
    private SessionAuthBusiness sessionAuthBusiness;
    
    /**
     * Deletes a provider connection.
     *
     * @param providerId The provider ID
     * @param request The delete request (optional body with cleanup options)
     * @param principal The authenticated user principal
     * @param authHeader The authorization header for Bearer token auth
     * @return ResponseEntity with deletion result
     */
    @DeleteMapping("/{providerId}")
    public ResponseEntity<DeleteProviderResponse> deleteProvider(
            @PathVariable String providerId,
            @Valid @RequestBody(required = false) DeleteProviderRequest request,
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        log.info("DeleteProvider: Principal userId: {}, AuthHeader present: {}",
                userId, authHeader != null);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    log.info("Provider delete authenticated via Bearer token for user: {}", userId);
                } else {
                    log.warn("Invalid Bearer token provided for provider delete");
                }
            } catch (Exception e) {
                log.warn("Error validating Bearer token for provider delete: {}", e.getMessage());
            }
        }

        if (userId == null) {
            log.error("No valid authentication session or token for provider delete");
            throw new StrategizException(
                ServiceProviderErrorDetails.PROVIDER_DATABASE_ERROR,
                getModuleName(),
                providerId,
                "User not authenticated"
            );
        }

        try {
            // Create request if not provided
            if (request == null) {
                request = new DeleteProviderRequest();
            }

            // Set user ID from authenticated session or token
            request.setUserId(userId);
            request.setProviderId(providerId);

            // Delegate to service
            DeleteProviderResponse response = deleteProviderService.deleteProvider(request);

            log.info("Successfully deleted provider {} for user {}", providerId, userId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error deleting provider {} for user {}: {}", providerId, userId, e.getMessage());

            DeleteProviderResponse errorResponse = new DeleteProviderResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorCode("VALIDATION_ERROR");
            errorResponse.setErrorMessage(e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error deleting provider {} for user {}: {}", providerId, userId, e.getMessage(), e);

            DeleteProviderResponse errorResponse = new DeleteProviderResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorCode("INTERNAL_ERROR");
            errorResponse.setErrorMessage("An unexpected error occurred");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 