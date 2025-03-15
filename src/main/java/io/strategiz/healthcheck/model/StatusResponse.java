package io.strategiz.healthcheck.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Response model for health check status
 */
public class StatusResponse {
    private String status;
    private String message;
    private long timestamp;

    public StatusResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Create a success response
     * 
     * @param message Success message
     * @return StatusResponse with success status
     */
    public static StatusResponse success(String message) {
        StatusResponse response = new StatusResponse();
        response.setStatus("success");
        response.setMessage(message);
        return response;
    }

    /**
     * Create an error response
     * 
     * @param message Error message
     * @return StatusResponse with error status
     */
    public static StatusResponse error(String message) {
        StatusResponse response = new StatusResponse();
        response.setStatus("error");
        response.setMessage(message);
        return response;
    }

    /**
     * Convert to a Map for use in ResponseEntity
     * 
     * @return Map representation of this response
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", this.status);
        map.put("message", this.message);
        map.put("timestamp", this.timestamp);
        return map;
    }
}
