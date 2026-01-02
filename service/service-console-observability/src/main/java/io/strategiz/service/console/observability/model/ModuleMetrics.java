package io.strategiz.service.console.observability.model;

/**
 * Aggregated metrics for a module (group of endpoints).
 * Modules are grouped by the first path segment (e.g., /v1/auth/* -> "auth" module).
 */
public class ModuleMetrics {
    private String moduleName;
    private Integer endpointCount = 0;
    private Long totalRequests = 0L;
    private Long successRequests = 0L;
    private Long errorRequests = 0L;
    private Double availability = 0.0;
    private Double worstP99LatencyMs = 0.0;

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Integer getEndpointCount() {
        return endpointCount;
    }

    public void setEndpointCount(Integer endpointCount) {
        this.endpointCount = endpointCount;
    }

    public Long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public Long getSuccessRequests() {
        return successRequests;
    }

    public void setSuccessRequests(Long successRequests) {
        this.successRequests = successRequests;
    }

    public Long getErrorRequests() {
        return errorRequests;
    }

    public void setErrorRequests(Long errorRequests) {
        this.errorRequests = errorRequests;
    }

    public Double getAvailability() {
        return availability;
    }

    public void setAvailability(Double availability) {
        this.availability = availability;
    }

    public Double getWorstP99LatencyMs() {
        return worstP99LatencyMs;
    }

    public void setWorstP99LatencyMs(Double worstP99LatencyMs) {
        this.worstP99LatencyMs = worstP99LatencyMs;
    }
}
