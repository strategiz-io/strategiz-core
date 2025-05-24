package io.strategiz.api.monitoring;

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

    public StatusResponse(String status, String message) {
        this.status = status;
        this.message = message;
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
     * Convert the status response to a map
     * @return Map representation of the status response
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("message", message);
        map.put("timestamp", timestamp);
        return map;
    }

    /**
     * Create a success status response
     * @param message Success message
     * @return StatusResponse with "success" status
     */
    public static StatusResponse success(String message) {
        return new StatusResponse("success", message);
    }

    /**
     * Create an error status response
     * @param message Error message
     * @return StatusResponse with "error" status
     */
    public static StatusResponse error(String message) {
        return new StatusResponse("error", message);
    }
}
