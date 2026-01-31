package io.strategiz.service.console.observability.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Metrics for a single REST endpoint. Tracks availability, latency, and error
 * information.
 */
public class EndpointMetrics {

	private String method;

	private String uri;

	private String endpoint;

	private Long totalRequests = 0L;

	private Long successRequests = 0L;

	private Long errorRequests = 0L;

	private Double availability = 0.0;

	private Double errorRate = 0.0;

	private Double latencyP50Ms = 0.0;

	private Double latencyP95Ms = 0.0;

	private Double latencyP99Ms = 0.0;

	private Double latencyMaxMs = 0.0;

	private Double latencyMeanMs = 0.0;

	private Map<String, Long> errorsByStatus = new HashMap<>();

	private Map<String, Long> errorsByException = new HashMap<>();

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
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

	public Double getErrorRate() {
		return errorRate;
	}

	public void setErrorRate(Double errorRate) {
		this.errorRate = errorRate;
	}

	public Double getLatencyP50Ms() {
		return latencyP50Ms;
	}

	public void setLatencyP50Ms(Double latencyP50Ms) {
		this.latencyP50Ms = latencyP50Ms;
	}

	public Double getLatencyP95Ms() {
		return latencyP95Ms;
	}

	public void setLatencyP95Ms(Double latencyP95Ms) {
		this.latencyP95Ms = latencyP95Ms;
	}

	public Double getLatencyP99Ms() {
		return latencyP99Ms;
	}

	public void setLatencyP99Ms(Double latencyP99Ms) {
		this.latencyP99Ms = latencyP99Ms;
	}

	public Double getLatencyMaxMs() {
		return latencyMaxMs;
	}

	public void setLatencyMaxMs(Double latencyMaxMs) {
		this.latencyMaxMs = latencyMaxMs;
	}

	public Double getLatencyMeanMs() {
		return latencyMeanMs;
	}

	public void setLatencyMeanMs(Double latencyMeanMs) {
		this.latencyMeanMs = latencyMeanMs;
	}

	public Map<String, Long> getErrorsByStatus() {
		return errorsByStatus;
	}

	public void setErrorsByStatus(Map<String, Long> errorsByStatus) {
		this.errorsByStatus = errorsByStatus;
	}

	public Map<String, Long> getErrorsByException() {
		return errorsByException;
	}

	public void setErrorsByException(Map<String, Long> errorsByException) {
		this.errorsByException = errorsByException;
	}

}
