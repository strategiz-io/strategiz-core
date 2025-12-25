package io.strategiz.client.execution.model;

import java.util.List;
import java.util.Map;

public class HealthResponse {
    private String status;
    private List<String> supportedLanguages;
    private int maxTimeoutSeconds;
    private int maxMemoryMb;
    private Map<String, String> metadata;

    public HealthResponse() {}

    public HealthResponse(String status, List<String> supportedLanguages, int maxTimeoutSeconds, 
                         int maxMemoryMb, Map<String, String> metadata) {
        this.status = status;
        this.supportedLanguages = supportedLanguages;
        this.maxTimeoutSeconds = maxTimeoutSeconds;
        this.maxMemoryMb = maxMemoryMb;
        this.metadata = metadata;
    }

    public static HealthResponseBuilder builder() {
        return new HealthResponseBuilder();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getSupportedLanguages() { return supportedLanguages; }
    public void setSupportedLanguages(List<String> supportedLanguages) { this.supportedLanguages = supportedLanguages; }
    public int getMaxTimeoutSeconds() { return maxTimeoutSeconds; }
    public void setMaxTimeoutSeconds(int maxTimeoutSeconds) { this.maxTimeoutSeconds = maxTimeoutSeconds; }
    public int getMaxMemoryMb() { return maxMemoryMb; }
    public void setMaxMemoryMb(int maxMemoryMb) { this.maxMemoryMb = maxMemoryMb; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public static class HealthResponseBuilder {
        private String status;
        private List<String> supportedLanguages;
        private int maxTimeoutSeconds;
        private int maxMemoryMb;
        private Map<String, String> metadata;

        public HealthResponseBuilder status(String status) { this.status = status; return this; }
        public HealthResponseBuilder supportedLanguages(List<String> supportedLanguages) { 
            this.supportedLanguages = supportedLanguages; return this; 
        }
        public HealthResponseBuilder maxTimeoutSeconds(int maxTimeoutSeconds) { 
            this.maxTimeoutSeconds = maxTimeoutSeconds; return this; 
        }
        public HealthResponseBuilder maxMemoryMb(int maxMemoryMb) { 
            this.maxMemoryMb = maxMemoryMb; return this; 
        }
        public HealthResponseBuilder metadata(Map<String, String> metadata) { 
            this.metadata = metadata; return this; 
        }
        public HealthResponse build() {
            return new HealthResponse(status, supportedLanguages, maxTimeoutSeconds, maxMemoryMb, metadata);
        }
    }
}
