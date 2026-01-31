package io.strategiz.service.console.observability.model;

/**
 * System-wide aggregated metrics across all endpoints.
 */
public class SystemMetrics {

	private Integer totalEndpoints = 0;

	private Long totalRequests = 0L;

	private Long successRequests = 0L;

	private Long errorRequests = 0L;

	private Double overallAvailability = 0.0;

	private Double worstP99LatencyMs = 0.0;

	public Integer getTotalEndpoints() {
		return totalEndpoints;
	}

	public void setTotalEndpoints(Integer totalEndpoints) {
		this.totalEndpoints = totalEndpoints;
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

	public Double getOverallAvailability() {
		return overallAvailability;
	}

	public void setOverallAvailability(Double overallAvailability) {
		this.overallAvailability = overallAvailability;
	}

	public Double getWorstP99LatencyMs() {
		return worstP99LatencyMs;
	}

	public void setWorstP99LatencyMs(Double worstP99LatencyMs) {
		this.worstP99LatencyMs = worstP99LatencyMs;
	}

}
