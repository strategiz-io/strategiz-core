package io.strategiz.service.base.model;

import java.io.Serializable;

/**
 * Base class for service response objects.
 * 
 * Note: 
 * - Headers are handled by StandardHeadersInterceptor
 * - Errors are handled by GlobalExceptionHandler with StandardErrorResponse
 * - Response models should contain only their specific data fields
 */
public abstract class BaseServiceResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Base class provides common structure - subclasses add their specific fields
}
