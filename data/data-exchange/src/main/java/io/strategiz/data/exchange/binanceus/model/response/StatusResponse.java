package io.strategiz.data.exchange.binanceus.model.response;

import java.util.Objects;

/**
 * Response model for status information
 */
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
    
    public StatusResponse(String status, String message, Long timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    // Static factory methods
    public static StatusResponse success(String message) {
        return new StatusResponse("success", message);
    }
    
    public static StatusResponse error(String message) {
        return new StatusResponse("error", message);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatusResponse that = (StatusResponse) o;
        return Objects.equals(status, that.status) &&
               Objects.equals(message, that.message) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, message, timestamp);
    }
    
    @Override
    public String toString() {
        return "StatusResponse{" +
               "status='" + status + '\'' +
               ", message='" + message + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
