package io.strategiz.service.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Service model for brokerage credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerageCredentialsResponse {
    
    private String userId;
    private String provider;
    private Map<String, String> credentials;
    private boolean active;
    private String lastUpdated;
    private boolean success;
    private String errorMessage;
}
