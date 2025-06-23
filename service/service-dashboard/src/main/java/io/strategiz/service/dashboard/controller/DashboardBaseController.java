package io.strategiz.service.dashboard.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * Base controller with common utilities for dashboard controllers.
 * Provides methods for response metadata creation and headers setup.
 */
public abstract class DashboardBaseController {
    
    /**
     * Create a success metadata object.
     * 
     * @return Map with success information
     */
    protected Map<String, Object> createSuccessMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("status", "success");
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }
    
    /**
     * Create an error metadata object.
     * 
     * @param errorCode Error code
     * @param errorMessage Error message
     * @return Map with error information
     */
    protected Map<String, Object> createErrorMetadata(String errorCode, String errorMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("status", "error");
        metadata.put("errorCode", errorCode);
        metadata.put("errorMessage", errorMessage);
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }
    
    /**
     * Create HTTP headers for JSON responses.
     * 
     * @return HTTP headers
     */
    protected HttpHeaders createJsonResponseHeaders() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return responseHeaders;
    }
}
