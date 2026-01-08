package io.strategiz.service.console.controller;

import io.strategiz.data.featureflags.entity.FeatureFlagEntity;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing feature flags.
 * Allows administrators to enable/disable features across the platform.
 *
 * FeatureFlagService dependency is optional - returns 503 when unavailable.
 */
@RestController
@RequestMapping("/v1/console/feature-flags")
@Tag(name = "Admin - Feature Flags", description = "Feature flag management for administrators")
public class AdminFeatureFlagController extends BaseController {

    private static final String MODULE_NAME = "CONSOLE";

    private final FeatureFlagService featureFlagService;

    @Autowired
    public AdminFeatureFlagController(@Autowired(required = false) FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
        log.info("AdminFeatureFlagController initialized - featureFlagService={}", featureFlagService != null);
    }

    private ResponseEntity<Map<String, Object>> serviceUnavailable() {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Feature flag service not available");
        error.put("status", 503);
        return ResponseEntity.status(503).body(error);
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    @GetMapping
    @Operation(summary = "Get all feature flags", description = "Returns all feature flags in the system")
    public ResponseEntity<Map<String, Object>> getAllFlags(HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getAllFlags", adminUserId);

        List<FeatureFlagEntity> flags = featureFlagService.getAllFlags();

        Map<String, Object> response = new HashMap<>();
        response.put("flags", flags);
        response.put("count", flags.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get flags by category", description = "Returns feature flags for a specific category")
    public ResponseEntity<Map<String, Object>> getFlagsByCategory(
            @PathVariable String category,
            HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getFlagsByCategory", adminUserId, Map.of("category", category));

        List<FeatureFlagEntity> flags = featureFlagService.getFlagsByCategory(category);

        Map<String, Object> response = new HashMap<>();
        response.put("flags", flags);
        response.put("category", category);
        response.put("count", flags.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{flagId}")
    @Operation(summary = "Get a specific flag", description = "Returns a specific feature flag by ID")
    public ResponseEntity<?> getFlag(
            @PathVariable String flagId,
            HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getFlag", adminUserId, Map.of("flagId", flagId));

        return featureFlagService.getFlag(flagId)
            .map(flag -> ResponseEntity.ok((Object) flag))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{flagId}/enabled")
    @Operation(summary = "Check if flag is enabled", description = "Quick check if a feature is enabled")
    public ResponseEntity<Map<String, Object>> isEnabled(
            @PathVariable String flagId,
            HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("isEnabled", adminUserId, Map.of("flagId", flagId));

        boolean enabled = featureFlagService.isEnabled(flagId);

        Map<String, Object> response = new HashMap<>();
        response.put("flagId", flagId);
        response.put("enabled", enabled);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{flagId}/enable")
    @Operation(summary = "Enable a feature flag", description = "Enables a specific feature flag")
    public ResponseEntity<?> enableFlag(
            @PathVariable String flagId,
            HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("enableFlag", adminUserId, Map.of("flagId", flagId));

        FeatureFlagEntity flag = featureFlagService.enableFlag(flagId);
        log.info("Feature flag {} enabled by admin {}", flagId, adminUserId);

        return ResponseEntity.ok(flag);
    }

    @PostMapping("/{flagId}/disable")
    @Operation(summary = "Disable a feature flag", description = "Disables a specific feature flag")
    public ResponseEntity<?> disableFlag(
            @PathVariable String flagId,
            HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("disableFlag", adminUserId, Map.of("flagId", flagId));

        FeatureFlagEntity flag = featureFlagService.disableFlag(flagId);
        log.info("Feature flag {} disabled by admin {}", flagId, adminUserId);

        return ResponseEntity.ok(flag);
    }

    @PutMapping("/{flagId}")
    @Operation(summary = "Update a feature flag", description = "Updates a feature flag's properties")
    public ResponseEntity<?> updateFlag(
            @PathVariable String flagId,
            @RequestBody FeatureFlagEntity flagUpdate,
            HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("updateFlag", adminUserId, Map.of("flagId", flagId));

        // Ensure flagId matches
        flagUpdate.setFlagId(flagId);
        FeatureFlagEntity flag = featureFlagService.updateFlag(flagUpdate);
        log.info("Feature flag {} updated by admin {}", flagId, adminUserId);

        return ResponseEntity.ok(flag);
    }

    @PostMapping
    @Operation(summary = "Create a new feature flag", description = "Creates a new feature flag")
    public ResponseEntity<?> createFlag(
            @RequestBody FeatureFlagEntity flag,
            HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("createFlag", adminUserId, Map.of("flagId", flag.getFlagId()));

        FeatureFlagEntity created = featureFlagService.createFlag(flag);
        log.info("Feature flag {} created by admin {}", flag.getFlagId(), adminUserId);

        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{flagId}")
    @Operation(summary = "Delete a feature flag", description = "Deletes a feature flag (use with caution)")
    public ResponseEntity<Map<String, Object>> deleteFlag(
            @PathVariable String flagId,
            HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("deleteFlag", adminUserId, Map.of("flagId", flagId));

        featureFlagService.deleteFlag(flagId);
        log.info("Feature flag {} deleted by admin {}", flagId, adminUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("flagId", flagId);
        response.put("deleted", true);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "Refresh flag cache", description = "Refreshes the in-memory feature flag cache")
    public ResponseEntity<Map<String, Object>> refreshCache(HttpServletRequest request) {
        if (featureFlagService == null) {
            return serviceUnavailable();
        }
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("refreshCache", adminUserId);

        featureFlagService.refreshCache();
        log.info("Feature flag cache refreshed by admin {}", adminUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Feature flag cache refreshed");

        return ResponseEntity.ok(response);
    }
}
