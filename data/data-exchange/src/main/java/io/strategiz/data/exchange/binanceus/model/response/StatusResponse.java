package io.strategiz.data.exchange.binanceus.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Response model for status information
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StatusResponse {
    private String status;
    private String message;
    private Long timestamp;
    
    public StatusResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public StatusResponse(String status, String message) {
        this();
        this.status = status;
        this.message = message;
    }
    
    public static StatusResponse success(String message) {
        return new StatusResponse("success", message);
    }
    
    public static StatusResponse error(String message) {
        return new StatusResponse("error", message);
    }
}
