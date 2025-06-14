package io.strategiz.api.base.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Common response metadata object for all API responses.
 * Contains status information, timestamps, and error details when applicable.
 */
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
    
    /**
     * Default constructor
     */
    public ResponseMetadata() {
    }
    
    /**
     * @return status of the response (success or error)
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * @param status status of the response (success or error)
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * @return timestamp of when the response was created
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * @param timestamp timestamp of when the response was created
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * @return error code in case of error
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * @param errorCode error code in case of error
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * @return error message in case of error
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * @param errorMessage error message in case of error
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseMetadata that = (ResponseMetadata) o;
        return timestamp == that.timestamp && 
               Objects.equals(status, that.status) && 
               Objects.equals(errorCode, that.errorCode) && 
               Objects.equals(errorMessage, that.errorMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, timestamp, errorCode, errorMessage);
    }
    
    @Override
    public String toString() {
        return "ResponseMetadata{" +
                "status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
