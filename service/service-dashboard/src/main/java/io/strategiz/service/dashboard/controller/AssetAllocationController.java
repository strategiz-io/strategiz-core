package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.dashboard.service.AssetAllocationService;
import io.strategiz.service.dashboard.model.assetallocation.AssetAllocationData;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.framework.exception.StrategizException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for asset allocation data.
 * Provides endpoints for retrieving asset allocation information.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/dashboard/allocation")
public class AssetAllocationController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return "service-dashboard";
    }
    
    private static final Logger log = LoggerFactory.getLogger(AssetAllocationController.class);

    private final AssetAllocationService assetAllocationService;
    
    public AssetAllocationController(AssetAllocationService assetAllocationService) {
        this.assetAllocationService = assetAllocationService;
    }
    
    /**
     * Get asset allocation data for a user.
     * 
     * @param userId The user ID to get data for
     * @return Clean asset allocation data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAssetAllocation(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving asset allocation for user: {}", userId);
        
        // Get data from service - let exceptions bubble up
        AssetAllocationData assetAllocation = assetAllocationService.getAssetAllocation(userId);
        
        // Check if data exists
        if (assetAllocation == null) {
            throw new StrategizException(ServiceDashboardErrorDetails.ASSET_ALLOCATION_NOT_FOUND, "service-dashboard", userId);
        }
        
        // Create response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("assetAllocation", assetAllocation);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(responseData);
    }
}
