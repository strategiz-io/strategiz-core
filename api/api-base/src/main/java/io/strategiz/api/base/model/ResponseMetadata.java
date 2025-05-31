package io.strategiz.api.base.model;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common response metadata object for all API responses.
 * Contains status information, timestamps, and error details when applicable.
 */
@Data
@NoArgsConstructor
public class ResponseMetadata implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Status of the response (success or error)
     */
    private String status;
    
    /**
     * Timestamp of when the response was created
     */
    private long timestamp;
    
    /**
     * Error code in case of error
     */
    private String errorCode;
    
    /**
     * Error message in case of error
     */
    private String errorMessage;
}
