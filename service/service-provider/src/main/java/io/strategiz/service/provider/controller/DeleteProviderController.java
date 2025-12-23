package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.model.request.DeleteProviderRequest;
import io.strategiz.service.provider.model.response.DeleteProviderResponse;
import io.strategiz.service.provider.service.DeleteProviderService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for deleting provider connections and data.
 * Handles HTTP requests for disconnecting providers and cleaning up associated data.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers")
@RequireAuth(minAcr = "1")
public class DeleteProviderController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.PROVIDER_MODULE;
    }

    @Autowired
    private DeleteProviderService deleteProviderService;
    
    /**
     * Deletes a provider connection.
     *
     * @param providerId The provider ID
     * @param request The delete request (optional body with cleanup options)
     * @param user The authenticated user from HTTP-only cookie
     * @return ResponseEntity with deletion result
     */
    @DeleteMapping("/{providerId}")
    public ResponseEntity<DeleteProviderResponse> deleteProvider(
            @PathVariable String providerId,
            @Valid @RequestBody(required = false) DeleteProviderRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        log.info("DeleteProvider: userId: {}, providerId: {}", userId, providerId);

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