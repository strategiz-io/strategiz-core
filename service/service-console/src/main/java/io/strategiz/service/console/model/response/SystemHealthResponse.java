package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Response model for system health summary.
 */
public class SystemHealthResponse {

	@JsonProperty("status")
	private String status; // UP, DOWN, DEGRADED

	@JsonProperty("uptime")
	private String uptime;

	@JsonProperty("startTime")
	private Instant startTime;

	@JsonProperty("version")
	private String version;

	@JsonProperty("activeUsers")
	private Integer activeUsers;

	@JsonProperty("activeSessions")
	private Integer activeSessions;

	@JsonProperty("memoryUsage")
	private MemoryUsage memoryUsage;

	@JsonProperty("components")
	private Map<String, ComponentHealth> components;

	// Nested classes
	public static class MemoryUsage {

		@JsonProperty("used")
		private Long used;

		@JsonProperty("max")
		private Long max;

		@JsonProperty("free")
		private Long free;

		@JsonProperty("usagePercent")
		private Double usagePercent;

		public MemoryUsage() {
		}

		public MemoryUsage(Long used, Long max, Long free) {
			this.used = used;
			this.max = max;
			this.free = free;
			this.usagePercent = max > 0 ? (used * 100.0 / max) : 0;
		}

		// Getters and Setters
		public Long getUsed() {
			return used;
		}

		public void setUsed(Long used) {
			this.used = used;
		}

		public Long getMax() {
			return max;
		}

		public void setMax(Long max) {
			this.max = max;
		}

		public Long getFree() {
			return free;
		}

		public void setFree(Long free) {
			this.free = free;
		}

		public Double getUsagePercent() {
			return usagePercent;
		}

		public void setUsagePercent(Double usagePercent) {
			this.usagePercent = usagePercent;
		}

	}

	public static class ComponentHealth {

		@JsonProperty("status")
		private String status;

		@JsonProperty("details")
		private Map<String, Object> details;

		public ComponentHealth() {
		}

		public ComponentHealth(String status) {
			this.status = status;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public Map<String, Object> getDetails() {
			return details;
		}

		public void setDetails(Map<String, Object> details) {
			this.details = details;
		}

	}

	// Constructors
	public SystemHealthResponse() {
	}

	// Getters and Setters
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUptime() {
		return uptime;
	}

	public void setUptime(String uptime) {
		this.uptime = uptime;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Integer getActiveUsers() {
		return activeUsers;
	}

	public void setActiveUsers(Integer activeUsers) {
		this.activeUsers = activeUsers;
	}

	public Integer getActiveSessions() {
		return activeSessions;
	}

	public void setActiveSessions(Integer activeSessions) {
		this.activeSessions = activeSessions;
	}

	public MemoryUsage getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(MemoryUsage memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public Map<String, ComponentHealth> getComponents() {
		return components;
	}

	public void setComponents(Map<String, ComponentHealth> components) {
		this.components = components;
	}

}
