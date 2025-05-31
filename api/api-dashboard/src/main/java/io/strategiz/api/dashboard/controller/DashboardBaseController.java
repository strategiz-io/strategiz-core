package io.strategiz.api.dashboard.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import io.strategiz.api.common.controller.BaseController;
import io.strategiz.api.base.model.ResponseMetadata;
import io.strategiz.service.base.model.BaseServiceResponse;

/**
 * Base controller with common utilities for dashboard controllers.
 * Provides methods for response metadata creation and headers setup.
 * Extends the common BaseController with Object as request type and BaseServiceResponse as response type.
 */
public abstract class DashboardBaseController extends BaseController<Object, BaseServiceResponse> {
    
    /**
     * Create a success metadata object.
     * 
     * @return ResponseMetadata with success information
     */
    protected ResponseMetadata createSuccessMetadata() {
        ResponseMetadata metadata = new ResponseMetadata();
        metadata.setStatus("success");
        metadata.setTimestamp(System.currentTimeMillis());
        return metadata;
    }
    
    /**
     * Create an error metadata object.
     * 
     * @param errorCode Error code
     * @param errorMessage Error message
     * @return ResponseMetadata with error information
     */
    protected ResponseMetadata createErrorMetadata(String errorCode, String errorMessage) {
        ResponseMetadata metadata = new ResponseMetadata();
        metadata.setStatus("error");
        metadata.setErrorCode(errorCode);
        metadata.setErrorMessage(errorMessage);
        metadata.setTimestamp(System.currentTimeMillis());
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
