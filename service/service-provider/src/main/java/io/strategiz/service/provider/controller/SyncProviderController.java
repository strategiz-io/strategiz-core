package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.service.SyncProviderService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
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

    @Autowired
    public SyncProviderController(SyncProviderService syncProviderService) {
        this.syncProviderService = syncProviderService;
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
            @PathVariable String providerId) {

        String userId = principal != null ? principal.getName() : null;

        if (userId == null) {
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
