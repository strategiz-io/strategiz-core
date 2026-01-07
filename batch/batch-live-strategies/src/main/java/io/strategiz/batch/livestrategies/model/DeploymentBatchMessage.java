package io.strategiz.batch.livestrategies.model;

import io.strategiz.business.livestrategies.model.SymbolSetGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Message published to Pub/Sub for batch processing of alerts and bots.
 *
 * Each message contains up to 100 symbol sets, where each symbol set includes:
 * - The symbols to fetch market data for
 * - The alert IDs to evaluate
 * - The bot IDs to evaluate
 */
public class DeploymentBatchMessage {

	private String messageId;

	private String tier;

	private List<SymbolSetGroup> symbolSets;

	private Instant createdAt;

	private int totalAlerts;

	private int totalBots;

	public DeploymentBatchMessage() {
		this.messageId = UUID.randomUUID().toString();
		this.createdAt = Instant.now();
	}

	public DeploymentBatchMessage(String tier, List<SymbolSetGroup> symbolSets) {
		this();
		this.tier = tier;
		this.symbolSets = symbolSets;
		this.totalAlerts = symbolSets.stream().mapToInt(g -> g.getAlertIds().size()).sum();
		this.totalBots = symbolSets.stream().mapToInt(g -> g.getBotIds().size()).sum();
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public List<SymbolSetGroup> getSymbolSets() {
		return symbolSets;
	}

	public void setSymbolSets(List<SymbolSetGroup> symbolSets) {
		this.symbolSets = symbolSets;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public int getTotalAlerts() {
		return totalAlerts;
	}

	public void setTotalAlerts(int totalAlerts) {
		this.totalAlerts = totalAlerts;
	}

	public int getTotalBots() {
		return totalBots;
	}

	public void setTotalBots(int totalBots) {
		this.totalBots = totalBots;
	}

	public static class Builder {

		private String tier;

		private List<SymbolSetGroup> symbolSets;

		public Builder tier(String tier) {
			this.tier = tier;
			return this;
		}

		public Builder symbolSets(List<SymbolSetGroup> symbolSets) {
			this.symbolSets = symbolSets;
			return this;
		}

		public DeploymentBatchMessage build() {
			return new DeploymentBatchMessage(tier, symbolSets);
		}

	}

	@Override
	public String toString() {
		return "DeploymentBatchMessage{" + "messageId='" + messageId + '\'' + ", tier='" + tier + '\''
				+ ", symbolSets=" + (symbolSets != null ? symbolSets.size() : 0) + ", totalAlerts=" + totalAlerts
				+ ", totalBots=" + totalBots + ", createdAt=" + createdAt + '}';
	}

}
