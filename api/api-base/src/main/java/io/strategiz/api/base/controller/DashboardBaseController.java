package io.strategiz.api.base.controller;

import io.strategiz.api.base.model.ResponseMetadata;
import io.strategiz.service.base.model.BaseServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * Base controller for all dashboard feature controllers.
 * Provides common methods for response metadata and headers creation.
 */
@Slf4j
public abstract class DashboardBaseController extends BaseController<Object, BaseServiceResponse> {

    /**
     * Creates HTTP headers for JSON responses.
     *
     * @return HttpHeaders configured for JSON
     */
    protected HttpHeaders createJsonResponseHeaders() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return responseHeaders;
    }

    /**
     * Creates a success metadata object.
     *
     * @return ResponseMetadata configured for success
     */
    protected ResponseMetadata createSuccessMetadata() {
        ResponseMetadata metadata = new ResponseMetadata();
        metadata.setStatus("success");
        metadata.setTimestamp(System.currentTimeMillis());
        return metadata;
    }

    /**
     * Creates an error metadata object.
     *
     * @param errorCode The error code
     * @param errorMessage The error message
     * @return ResponseMetadata configured with error details
     */
    protected ResponseMetadata createErrorMetadata(String errorCode, String errorMessage) {
        ResponseMetadata metadata = new ResponseMetadata();
        metadata.setStatus("error");
        metadata.setTimestamp(System.currentTimeMillis());
        metadata.setErrorCode(errorCode);
        metadata.setErrorMessage(errorMessage);
        return metadata;
    }
    
    /**
     * Converts a ResponseMetadata object to a Map for use with BaseServiceResponse.
     *
     * @param metadata The ResponseMetadata object
     * @return A Map containing the metadata values
     */
    protected Map<String, Object> convertMetadataToMap(ResponseMetadata metadata) {
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("status", metadata.getStatus());
        metadataMap.put("timestamp", metadata.getTimestamp());
        
        if (metadata.getErrorCode() != null) {
            metadataMap.put("errorCode", metadata.getErrorCode());
        }
        
        if (metadata.getErrorMessage() != null) {
            metadataMap.put("errorMessage", metadata.getErrorMessage());
        }
        
        return metadataMap;
    }
}
