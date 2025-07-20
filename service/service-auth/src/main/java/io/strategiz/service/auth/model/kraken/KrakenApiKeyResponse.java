package io.strategiz.service.auth.model.kraken;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for Kraken API key operations
 */
@Schema(description = "Kraken API operation response")
public class KrakenApiKeyResponse {
    
    @Schema(description = "Whether the operation was successful", example = "true")
    private boolean success;
    
    @Schema(description = "Response message", example = "Kraken credentials stored successfully")
    private String message;
    
    public KrakenApiKeyResponse() {}
    
    public KrakenApiKeyResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean success;
        private String message;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public KrakenApiKeyResponse build() {
            return new KrakenApiKeyResponse(success, message);
        }
    }
}