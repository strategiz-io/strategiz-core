package io.strategiz.service.portfolio.model.request;

/**
 * Request DTO for portfolio AI analysis.
 * Used to specify which type of insight to generate and which LLM model to use.
 */
public class PortfolioAnalysisRequestDto {

	private String insightType; // "risk", "performance", "rebalancing", "opportunities", or "all"

	private String providerId; // Optional: analyze specific provider/account only

	private String model; // Optional: LLM model selection (default: gemini-2.5-flash)

	public String getInsightType() {
		return insightType;
	}

	public void setInsightType(String insightType) {
		this.insightType = insightType;
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

}
