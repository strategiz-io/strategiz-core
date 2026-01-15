package io.strategiz.service.console.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents the execution history of a Platform Agent
 */
public record AgentHistory(String agentId, List<AgentExecution> executions, int totalExecutions, int page,
		int pageSize) {
	public record AgentExecution(String executionId, Instant timestamp, String status, // SUCCESS,
																						// FAILED,
																						// IN_PROGRESS
			String trigger, // SCHEDULED, MANUAL, WEBHOOK
			String triggeredBy, ExecutionResult result, String workflowRunUrl, String pullRequestUrl) {
	}

	public record ExecutionResult(int dependenciesUpdated, int vulnerabilitiesFixed, boolean testsResult,
			boolean healthCheckPassed, String summary) {
	}
}
