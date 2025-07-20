package io.strategiz.service.auth.model.kraken;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for storing Kraken API credentials
 */
@Schema(description = "Kraken API key credentials request")
public class KrakenApiKeyRequest {
    
    @NotBlank(message = "API key is required")
    @Schema(description = "Kraken API public key", example = "your-api-key-here", required = true)
    private String apiKey;
    
    @NotBlank(message = "API secret is required")
    @Schema(description = "Kraken API private secret (base64 encoded)", example = "your-api-secret-here", required = true)
    private String apiSecret;
    
    @Schema(description = "Optional one-time password for 2FA (if enabled on the API key)", example = "123456")
    private String otp;
    
    public KrakenApiKeyRequest() {}
    
    public KrakenApiKeyRequest(String apiKey, String apiSecret, String otp) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.otp = otp;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
    
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }
    
    public String getOtp() {
        return otp;
    }
    
    public void setOtp(String otp) {
        this.otp = otp;
    }
}