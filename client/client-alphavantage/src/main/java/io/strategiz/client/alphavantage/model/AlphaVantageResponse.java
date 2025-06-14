package io.strategiz.client.alphavantage.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base response model for AlphaVantage API responses.
 * Simple POJO that follows the pattern used in other client modules.
 */
public class AlphaVantageResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Constructors
    public AlphaVantageResponse() {}
    
    public AlphaVantageResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public AlphaVantageResponse(boolean success, String message, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlphaVantageResponse that = (AlphaVantageResponse) o;
        return success == that.success &&
               Objects.equals(message, that.message) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, message, timestamp);
    }
    
    @Override
    public String toString() {
        return "AlphaVantageResponse{" +
               "success=" + success +
               ", message='" + message + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
