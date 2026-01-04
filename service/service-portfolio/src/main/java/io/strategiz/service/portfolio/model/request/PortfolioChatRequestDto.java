package io.strategiz.service.portfolio.model.request;

import io.strategiz.business.aichat.model.ChatMessage;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request DTO for portfolio AI chat.
 * Supports conversational interaction about the user's portfolio.
 */
public class PortfolioChatRequestDto {

	@NotBlank(message = "Message is required")
	private String message;

	private String providerId; // Optional: context for specific account

	private String model; // Optional: LLM model selection

	private List<ChatMessage> conversationHistory;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public List<ChatMessage> getConversationHistory() {
		return conversationHistory;
	}

	public void setConversationHistory(List<ChatMessage> conversationHistory) {
		this.conversationHistory = conversationHistory;
	}

}
