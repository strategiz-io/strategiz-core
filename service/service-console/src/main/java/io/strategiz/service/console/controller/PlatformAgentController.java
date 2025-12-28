package io.strategiz.service.console.controller;

import io.strategiz.service.console.model.AgentHistory;
import io.strategiz.service.console.model.AgentStatus;
import io.strategiz.service.console.service.PlatformAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing Platform Agents
 */
@RestController
@RequestMapping("/v1/console/agents")
public class PlatformAgentController {

    private final PlatformAgentService agentService;

    private static final Map<String, String> AGENT_REPOS = Map.of(
            "security-agent-backend", "strategiz-io/strategiz-core",
            "security-agent-frontend", "strategiz-io/strategiz-ui"
    );

    public PlatformAgentController(PlatformAgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * GET /v1/console/agents
     * Get all configured agents
     */
    @GetMapping
    public ResponseEntity<List<AgentStatus>> getAllAgents() {
        List<AgentStatus> agents = agentService.getAllAgents();
        return ResponseEntity.ok(agents);
    }

    /**
     * GET /v1/console/agents/{agentId}
     * Get status of a specific agent
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<AgentStatus> getAgentStatus(@PathVariable String agentId) {
        String repository = AGENT_REPOS.get(agentId);
        if (repository == null) {
            return ResponseEntity.notFound().build();
        }

        AgentStatus status = agentService.getAgentStatus(agentId, repository);
        return ResponseEntity.ok(status);
    }

    /**
     * GET /v1/console/agents/{agentId}/history
     * Get execution history for an agent
     */
    @GetMapping("/{agentId}/history")
    public ResponseEntity<AgentHistory> getAgentHistory(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        String repository = AGENT_REPOS.get(agentId);
        if (repository == null) {
            return ResponseEntity.notFound().build();
        }

        AgentHistory history = agentService.getAgentHistory(agentId, repository, page, pageSize);
        return ResponseEntity.ok(history);
    }

    /**
     * POST /v1/console/agents/{agentId}/trigger
     * Manually trigger an agent to run
     */
    @PostMapping("/{agentId}/trigger")
    public ResponseEntity<Map<String, Object>> triggerAgent(
            @PathVariable String agentId,
            @RequestBody(required = false) Map<String, String> request
    ) {
        String repository = AGENT_REPOS.get(agentId);
        if (repository == null) {
            return ResponseEntity.notFound().build();
        }

        String triggeredBy = request != null ? request.get("triggeredBy") : "admin";
        boolean success = agentService.triggerAgent(agentId, repository, triggeredBy);

        if (success) {
            return ResponseEntity.ok(Map.of(
                    "status", "triggered",
                    "agentId", agentId,
                    "message", "Agent workflow has been triggered successfully"
            ));
        } else {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "agentId", agentId,
                    "message", "Failed to trigger agent workflow"
            ));
        }
    }

    /**
     * PUT /v1/console/agents/{agentId}/enabled
     * Enable or disable an agent
     */
    @PutMapping("/{agentId}/enabled")
    public ResponseEntity<Map<String, Object>> setAgentEnabled(
            @PathVariable String agentId,
            @RequestBody Map<String, Boolean> request
    ) {
        String repository = AGENT_REPOS.get(agentId);
        if (repository == null) {
            return ResponseEntity.notFound().build();
        }

        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing 'enabled' field in request body"
            ));
        }

        boolean success = agentService.setAgentEnabled(agentId, enabled);

        if (success) {
            return ResponseEntity.ok(Map.of(
                    "status", "updated",
                    "agentId", agentId,
                    "enabled", enabled,
                    "message", "Agent " + (enabled ? "enabled" : "disabled") + " successfully"
            ));
        } else {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "agentId", agentId,
                    "message", "Failed to update agent status"
            ));
        }
    }
}
