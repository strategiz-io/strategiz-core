package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.service.SyncProviderService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

/**
 * Controller for provider-specific synchronization operations.
 * Handles manual sync requests for individual providers (e.g., Coinbase, Kraken).
 *
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers")
public class SyncProviderController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.PROVIDER_MODULE;
    }

    private final SyncProviderService syncProviderService;
    private final SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    public SyncProviderController(SyncProviderService syncProviderService,
                                 SessionAuthBusiness sessionAuthBusiness) {
        this.syncProviderService = syncProviderService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    /**
     * Sync a specific provider's data.
     * This endpoint allows syncing individual providers without affecting others.
     *
     * @param principal The authenticated user principal
     * @param providerId The provider to sync (e.g., "coinbase", "kraken")
     * @return Sync result with status and updated data
     */
    @PostMapping("/{providerId}/sync")
    public ResponseEntity<Map<String, Object>> syncProvider(
            Principal principal,
            @PathVariable String providerId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Try to extract user ID from the principal (session-based auth) first
        String userId = principal != null ? principal.getName() : null;
        log.info("SyncProvider: Principal userId: {}, AuthHeader present: {}",
                userId, authHeader != null);

        // If session auth failed, try token-based auth as fallback
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var validationResult = sessionAuthBusiness.validateToken(token);
                if (validationResult.isPresent()) {
                    userId = validationResult.get().getUserId();
                    log.info("Provider sync authenticated via Bearer token for user: {}", userId);
                } else {
                    log.warn("Invalid Bearer token provided for provider sync");
                }
            } catch (Exception e) {
                log.warn("Error validating Bearer token for provider sync: {}", e.getMessage());
            }
        }

        if (userId == null) {
            log.error("No valid authentication session or token for provider sync");
            throw new StrategizException(
                ServiceProviderErrorDetails.PROVIDER_DATA_SYNC_FAILED,
                getModuleName(),
                providerId,
                "User not authenticated"
            );
        }

        try {
            // Sync the specific provider
            Map<String, Object> syncResult = syncProviderService.syncProvider(userId, providerId);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "providerId", providerId,
                "syncResult", syncResult,
                "timestamp", Instant.now().toString()
            ));

        } catch (StrategizException e) {
            throw e; // Re-throw to be handled by global exception handler

        } catch (Exception e) {
            throw new StrategizException(
                ServiceProviderErrorDetails.PROVIDER_DATA_SYNC_FAILED,
                getModuleName(),
                providerId,
                e.getMessage()
            );
        }
    }
}
