package io.strategiz.service.agents.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * DTO for AI Agent chat request
 */
public class AgentChatRequest {

    @JsonProperty("message")
    @NotBlank(message = "Message is required")
    private String message;

    @JsonProperty("agentId")
    private String agentId;

    @JsonProperty("conversationHistory")
    private List<AgentChatMessage> conversationHistory;

    @JsonProperty("model")
    private String model;

    @JsonProperty("additionalContext")
    private Map<String, Object> additionalContext;

    public AgentChatRequest() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public List<AgentChatMessage> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<AgentChatMessage> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Map<String, Object> getAdditionalContext() {
        return additionalContext;
    }

    public void setAdditionalContext(Map<String, Object> additionalContext) {
        this.additionalContext = additionalContext;
    }

}
