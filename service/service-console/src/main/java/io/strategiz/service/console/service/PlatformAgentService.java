package io.strategiz.service.console.service;

import io.strategiz.client.github.GitHubAppAuthClient;
import io.strategiz.client.github.GitHubAppConfig;
import io.strategiz.service.console.model.AgentHistory;
import io.strategiz.service.console.model.AgentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for managing Platform Agents - automated security and maintenance tasks
 */
@Service
public class PlatformAgentService {

    private static final Logger log = LoggerFactory.getLogger(PlatformAgentService.class);

    private final RestTemplate restTemplate;
    private final GitHubAppAuthClient githubAppAuthClient;
    private final GitHubAppConfig githubAppConfig;
    private final String githubApiUrl = "https://api.github.com";

    @Autowired
    public PlatformAgentService(
            RestTemplate restTemplate,
            GitHubAppAuthClient githubAppAuthClient,
            GitHubAppConfig githubAppConfig
    ) {
        this.restTemplate = restTemplate;
        this.githubAppAuthClient = githubAppAuthClient;
        this.githubAppConfig = githubAppConfig;
    }

    /**
     * Get list of all configured agents
     */
    public List<AgentStatus> getAllAgents() {
        List<AgentStatus> agents = new ArrayList<>();

        // Backend Security Agent
        agents.add(getAgentStatus("security-agent-backend", "strategiz-io/strategiz-core"));

        // Frontend Security Agent
        agents.add(getAgentStatus("security-agent-frontend", "strategiz-io/strategiz-ui"));

        return agents;
    }

    /**
     * Get status of a specific agent
     */
    public AgentStatus getAgentStatus(String agentId, String repository) {
        try {
            // Fetch workflow runs from GitHub API
            String url = String.format("%s/repos/%s/actions/workflows/security-agent.yml/runs?per_page=10",
                    githubApiUrl, repository);

            HttpHeaders headers = createAuthHeaders(repository);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> data = response.getBody();
            List<Map<String, Object>> workflowRuns = (List<Map<String, Object>>) data.get("workflow_runs");

            // Calculate metrics from workflow runs
            AgentStatus.AgentMetrics metrics = calculateMetrics(workflowRuns);

            // Get last run info
            Instant lastRun = null;
            String status = "IDLE";

            if (workflowRuns != null && !workflowRuns.isEmpty()) {
                Map<String, Object> lastWorkflow = workflowRuns.get(0);
                lastRun = Instant.parse((String) lastWorkflow.get("updated_at"));
                String conclusion = (String) lastWorkflow.get("conclusion");
                String workflowStatus = (String) lastWorkflow.get("status");

                if ("in_progress".equals(workflowStatus) || "queued".equals(workflowStatus)) {
                    status = "RUNNING";
                } else if ("success".equals(conclusion)) {
                    status = "IDLE";
                } else if ("failure".equals(conclusion)) {
                    status = "FAILED";
                }
            }

            // Calculate next scheduled run (every 6 hours)
            Instant nextRun = lastRun != null ? lastRun.plus(6, ChronoUnit.HOURS) : Instant.now();

            String name = agentId.contains("backend") ? "Backend Security Agent" : "Frontend Security Agent";
            String description = agentId.contains("backend")
                ? "Monitors and updates Maven dependencies for security vulnerabilities"
                : "Monitors and updates NPM dependencies for security vulnerabilities";

            return new AgentStatus(
                    agentId,
                    name,
                    description,
                    status,
                    true, // enabled by default
                    lastRun,
                    nextRun,
                    metrics,
                    repository
            );

        } catch (Exception e) {
            log.error("Error fetching agent status for {}: {}", agentId, e.getMessage());
            return createDefaultAgentStatus(agentId, repository);
        }
    }

    /**
     * Get execution history for an agent
     */
    public AgentHistory getAgentHistory(String agentId, String repository, int page, int pageSize) {
        try {
            String url = String.format("%s/repos/%s/actions/workflows/security-agent.yml/runs?page=%d&per_page=%d",
                    githubApiUrl, repository, page + 1, pageSize);

            HttpHeaders headers = createAuthHeaders(repository);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> data = response.getBody();
            List<Map<String, Object>> workflowRuns = (List<Map<String, Object>>) data.get("workflow_runs");
            int totalCount = (int) data.get("total_count");

            List<AgentHistory.AgentExecution> executions = new ArrayList<>();
            if (workflowRuns != null) {
                for (Map<String, Object> run : workflowRuns) {
                    executions.add(mapWorkflowRunToExecution(run, repository));
                }
            }

            return new AgentHistory(agentId, executions, totalCount, page, pageSize);

        } catch (Exception e) {
            log.error("Error fetching agent history for {}: {}", agentId, e.getMessage());
            return new AgentHistory(agentId, List.of(), 0, page, pageSize);
        }
    }

    /**
     * Trigger an agent to run now
     */
    public boolean triggerAgent(String agentId, String repository, String triggeredBy) {
        try {
            if (!githubAppConfig.isConfigured()) {
                log.error("GitHub App not configured, cannot trigger workflow");
                return false;
            }

            String url = String.format("%s/repos/%s/actions/workflows/security-agent.yml/dispatches",
                    githubApiUrl, repository);

            HttpHeaders headers = createAuthHeaders(repository);
            headers.set("Content-Type", "application/json");

            Map<String, Object> payload = Map.of(
                    "ref", "main",
                    "inputs", Map.of("force_update", "false")
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            log.info("Successfully triggered {} by {}", agentId, triggeredBy);
            return true;

        } catch (Exception e) {
            log.error("Error triggering agent {}: {}", agentId, e.getMessage());
            return false;
        }
    }

    /**
     * Enable or disable an agent
     */
    public boolean setAgentEnabled(String agentId, boolean enabled) {
        // For now, this is a placeholder
        // In a real implementation, this would update a configuration store
        // or use GitHub's workflow enable/disable API
        log.info("Agent {} {}", agentId, enabled ? "enabled" : "disabled");
        return true;
    }

    // Helper methods

    private AgentStatus.AgentMetrics calculateMetrics(List<Map<String, Object>> workflowRuns) {
        if (workflowRuns == null || workflowRuns.isEmpty()) {
            return new AgentStatus.AgentMetrics(0, 0, 0, 0, 0, 0.0);
        }

        int total = workflowRuns.size();
        int successful = 0;
        int failed = 0;

        for (Map<String, Object> run : workflowRuns) {
            String conclusion = (String) run.get("conclusion");
            if ("success".equals(conclusion)) {
                successful++;
            } else if ("failure".equals(conclusion)) {
                failed++;
            }
        }

        double successRate = total > 0 ? (successful * 100.0 / total) : 0.0;

        // Estimate PRs created (rough estimate based on successful runs)
        int prsCreated = (int) (successful * 0.3); // Assume 30% of runs create PRs

        // Estimate vulnerabilities fixed (rough estimate)
        int vulnerabilitiesFixed = prsCreated * 2; // Assume 2 vulnerabilities per PR on average

        return new AgentStatus.AgentMetrics(total, successful, failed, prsCreated, vulnerabilitiesFixed, successRate);
    }

    private AgentHistory.AgentExecution mapWorkflowRunToExecution(Map<String, Object> run, String repository) {
        String runId = String.valueOf(run.get("id"));
        Instant timestamp = Instant.parse((String) run.get("created_at"));
        String workflowStatus = (String) run.get("status");
        String conclusion = (String) run.get("conclusion");
        String triggerEvent = (String) run.get("event");

        String status = "IN_PROGRESS";
        if ("completed".equals(workflowStatus)) {
            status = "success".equals(conclusion) ? "SUCCESS" : "FAILED";
        }

        String trigger = "workflow_dispatch".equals(triggerEvent) ? "MANUAL" : "SCHEDULED";
        String triggeredBy = "system"; // Could be extracted from run.actor if available

        // Get workflow run URL
        String workflowRunUrl = (String) run.get("html_url");

        // Try to find associated PR
        String pullRequestUrl = findAssociatedPR(repository, timestamp);

        AgentHistory.ExecutionResult result = new AgentHistory.ExecutionResult(
                0, // Would need to parse logs to get actual numbers
                0,
                "success".equals(conclusion),
                "success".equals(conclusion),
                conclusion != null ? conclusion : "Running"
        );

        return new AgentHistory.AgentExecution(
                runId,
                timestamp,
                status,
                trigger,
                triggeredBy,
                result,
                workflowRunUrl,
                pullRequestUrl
        );
    }

    private String findAssociatedPR(String repository, Instant timestamp) {
        try {
            // Look for PRs created around the same time as the workflow run
            String url = String.format("%s/repos/%s/pulls?state=all&per_page=10&sort=created&direction=desc",
                    githubApiUrl, repository);

            HttpHeaders headers = createAuthHeaders(repository);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            List<Map<String, Object>> prs = response.getBody();
            if (prs != null) {
                for (Map<String, Object> pr : prs) {
                    String title = (String) pr.get("title");
                    if (title != null && title.contains("Security Agent")) {
                        return (String) pr.get("html_url");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not find associated PR: {}", e.getMessage());
        }
        return null;
    }

    private AgentStatus createDefaultAgentStatus(String agentId, String repository) {
        String name = agentId.contains("backend") ? "Backend Security Agent" : "Frontend Security Agent";
        String description = agentId.contains("backend")
            ? "Monitors and updates Maven dependencies for security vulnerabilities"
            : "Monitors and updates NPM dependencies for security vulnerabilities";

        return new AgentStatus(
                agentId,
                name,
                description,
                "IDLE",
                true,
                null,
                Instant.now(),
                new AgentStatus.AgentMetrics(0, 0, 0, 0, 0, 0.0),
                repository
        );
    }

    /**
     * Create HTTP headers with GitHub App authentication
     */
    private HttpHeaders createAuthHeaders(String repository) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");

        if (githubAppConfig.isConfigured()) {
            // Parse repository string (format: "owner/repo")
            String[] parts = repository.split("/");
            if (parts.length == 2) {
                String owner = parts[0];
                String repo = parts[1];

                // Get installation access token from GitHub App
                String token = githubAppAuthClient.getInstallationToken(owner, repo);
                if (token != null) {
                    headers.set("Authorization", "Bearer " + token);
                } else {
                    log.warn("Failed to obtain GitHub App installation token for {}", repository);
                }
            } else {
                log.error("Invalid repository format: {}. Expected 'owner/repo'", repository);
            }
        } else {
            log.warn("GitHub App not configured - API calls may fail");
        }

        return headers;
    }
}
