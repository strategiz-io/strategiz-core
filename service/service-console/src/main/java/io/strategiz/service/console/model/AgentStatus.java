package io.strategiz.service.console.model;

import java.time.Instant;

/**
 * Represents the current status of a Platform Agent
 */
public record AgentStatus(String agentId, String name, String description, String status, // RUNNING,
																							// IDLE,
																							// FAILED,
																							// DISABLED
		boolean enabled, Instant lastRun, Instant nextScheduledRun, AgentMetrics metrics, String repository) {
	public record AgentMetrics(int totalRuns, int successfulRuns, int failedRuns, int prsCreated,
			int vulnerabilitiesFixed, double successRate) {
	}
}
