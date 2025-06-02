package io.strategiz.api.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.api.dashboard.mapper.AssetAllocationResponseBuilder;
import io.strategiz.api.dashboard.model.assetallocation.AssetAllocationResponse;
import io.strategiz.api.dashboard.controller.DashboardBaseController;
import io.strategiz.service.base.model.BaseServiceResponse;
import io.strategiz.service.dashboard.AssetAllocationService;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for asset allocation data.
 * Provides endpoints for retrieving asset allocation information.
 */
@RestController
@RequestMapping("/api/dashboard/asset-allocation")
@Slf4j
public class AssetAllocationController extends DashboardBaseController {
    
    @Override
    public BaseServiceResponse process(Object request) {
        // Default implementation
        String userId = request instanceof String ? (String) request : "test-user";
        io.strategiz.service.dashboard.model.assetallocation.AssetAllocationData serviceData = 
            assetAllocationService.getAssetAllocation(userId);
        AssetAllocationResponse response = responseBuilder.buildAssetAllocationResponse(serviceData);
        response.setMetadata(convertMetadataToMap(createSuccessMetadata()));
        return response;
    }
    
    private final AssetAllocationService assetAllocationService;
    private final AssetAllocationResponseBuilder responseBuilder;
    
    @Autowired
    public AssetAllocationController(AssetAllocationService assetAllocationService, 
                                AssetAllocationResponseBuilder responseBuilder) {
        this.assetAllocationService = assetAllocationService;
        this.responseBuilder = responseBuilder;
    }
    
    /**
     * Get asset allocation data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with AssetAllocationResponse
     */
    @GetMapping
    public ResponseEntity<?> getAssetAllocation(@RequestParam(required = false) String userId) {
        // Set response headers
        HttpHeaders responseHeaders = createJsonResponseHeaders();
        
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            // Get data from service
            io.strategiz.service.dashboard.model.assetallocation.AssetAllocationData serviceData = 
                assetAllocationService.getAssetAllocation(userId);
            
            // Create and populate API response using the builder
            AssetAllocationResponse dashboardResponse = responseBuilder.buildAssetAllocationResponse(serviceData);
            dashboardResponse.setMetadata(convertMetadataToMap(createSuccessMetadata()));
            
            // Check if there is meaningful data to display
            if (dashboardResponse.getAllocations() == null || dashboardResponse.getAllocations().isEmpty()) {
                // Return 204 No Content if there's no data
                return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
            }
            
            // Return successful response with headers
            return new ResponseEntity<>(dashboardResponse, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving asset allocation: {}", e.getMessage(), e);
            
            // Create error response
            AssetAllocationResponse errorResponse = new AssetAllocationResponse();
            errorResponse.setMetadata(convertMetadataToMap(createErrorMetadata("ASSET_ALLOCATION_ERROR", e.getMessage())));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
