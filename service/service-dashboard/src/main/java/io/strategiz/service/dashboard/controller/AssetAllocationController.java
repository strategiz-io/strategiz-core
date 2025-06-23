package io.strategiz.service.dashboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.base.controller.BaseApiController;
import io.strategiz.service.base.model.ApiResponseWrapper;
import io.strategiz.service.dashboard.AssetAllocationService;
import io.strategiz.service.dashboard.model.assetallocation.AssetAllocationData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for asset allocation data.
 * Provides endpoints for retrieving asset allocation information.
 */
@RestController
@RequestMapping("/api/dashboard/asset-allocation")
public class AssetAllocationController extends BaseApiController {
    
    private static final Logger log = LoggerFactory.getLogger(AssetAllocationController.class);
    
    private final AssetAllocationService assetAllocationService;
    
    public AssetAllocationController(AssetAllocationService assetAllocationService) {
        this.assetAllocationService = assetAllocationService;
    }
    
    /**
     * Get asset allocation data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with asset allocation data
     */
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getAssetAllocation(@RequestParam(required = false) String userId) {
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            log.info("Retrieving asset allocation for user: {}", userId);
            
            // Get data from service
            AssetAllocationData assetAllocationData = assetAllocationService.getAssetAllocation(userId);
            
            // Create response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("assetAllocation", assetAllocationData);
            
            // Return successful response
            return success(responseData);
                
        } catch (Exception e) {
            log.error("Error retrieving asset allocation data", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, 
                "ASSET_ALLOCATION_ERROR", 
                "Error retrieving asset allocation data: " + e.getMessage());
        }
    }
}
