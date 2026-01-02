package io.strategiz.service.console.service;

// Temporarily disabled: client-github module not building in Cloud Build
// import io.strategiz.client.github.GitHubAppAuthClient;
// import io.strategiz.client.github.GitHubAppConfig;
import io.strategiz.service.console.model.AgentHistory;
import io.strategiz.service.console.model.AgentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing Platform Agents - automated security and maintenance tasks
 *
 * TEMPORARILY DISABLED: client-github module not building in Cloud Build
 */
@Service
public class PlatformAgentService {

    private static final Logger log = LoggerFactory.getLogger(PlatformAgentService.class);

    private final RestTemplate restTemplate;

    @Autowired
    public PlatformAgentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.warn("Platform Agents temporarily disabled - client-github module not available");
    }

    public List<AgentStatus> getAllAgents() {
        log.warn("Platform Agents temporarily disabled - client-github module not available");
        return new ArrayList<>();
    }

    public AgentStatus getAgentStatus(String agentId, String repository) {
        log.warn("Platform Agent '{}' status request - temporarily disabled", agentId);
        return createDefaultAgentStatus(agentId, repository);
    }

    public AgentHistory getAgentHistory(String agentId, String repository, int page, int pageSize) {
        log.warn("Platform Agent '{}' history request - temporarily disabled", agentId);
        return new AgentHistory(new ArrayList<>(), 0, 1, 10, 0);
    }

    public boolean triggerAgent(String agentId, String repository, String triggeredBy) {
        log.warn("Platform Agent '{}' trigger request - temporarily disabled", agentId);
        return false;
    }

    public boolean setAgentEnabled(String agentId, boolean enabled) {
        log.warn("Platform Agent '{}' enable/disable - temporarily disabled", agentId);
        return false;
    }

    private AgentStatus createDefaultAgentStatus(String agentId, String repository) {
        AgentStatus.AgentMetrics metrics = new AgentStatus.AgentMetrics(
                0, 0, 0, 0, 0.0, 0, 0
        );

        return new AgentStatus(
                agentId,
                "Platform Agent (" + agentId + ")",
                "Temporarily disabled",
                repository,
                AgentStatus.AgentRunStatus.DISABLED,
                null,
                0,
                metrics
        );
    }
}
