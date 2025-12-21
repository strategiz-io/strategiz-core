package io.strategiz.service.console.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.response.ProviderStatusResponse;
import io.strategiz.service.console.service.ProviderStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for provider integration status.
 */
@RestController
@RequestMapping("/v1/admin/providers")
@Tag(name = "Admin - Providers", description = "Provider status endpoints for administrators")
public class AdminProviderController extends BaseController {

    private static final String MODULE_NAME = "CONSOLE";

    private final ProviderStatusService providerStatusService;

    @Autowired
    public AdminProviderController(ProviderStatusService providerStatusService) {
        this.providerStatusService = providerStatusService;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    @GetMapping
    @Operation(summary = "List all providers", description = "Returns status of all integrated providers")
    public ResponseEntity<List<ProviderStatusResponse>> listProviders(HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("listProviders", adminUserId);

        List<ProviderStatusResponse> providers = providerStatusService.listProviders();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get provider status", description = "Returns status for a specific provider")
    public ResponseEntity<ProviderStatusResponse> getProvider(
            @Parameter(description = "Provider name") @PathVariable String name,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getProvider", adminUserId, "providerName=" + name);

        ProviderStatusResponse provider = providerStatusService.getProvider(name);
        return ResponseEntity.ok(provider);
    }

    @PostMapping("/{name}/sync")
    @Operation(summary = "Trigger provider sync", description = "Triggers a sync operation for the specified provider")
    public ResponseEntity<ProviderStatusResponse> syncProvider(
            @Parameter(description = "Provider name") @PathVariable String name,
            HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("syncProvider", adminUserId, "providerName=" + name);

        ProviderStatusResponse provider = providerStatusService.syncProvider(name);
        log.info("Provider {} sync triggered by admin {}", name, adminUserId);
        return ResponseEntity.ok(provider);
    }
}
