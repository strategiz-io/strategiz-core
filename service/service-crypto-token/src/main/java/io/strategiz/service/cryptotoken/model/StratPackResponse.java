package io.strategiz.service.cryptotoken.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.preferences.entity.StratPackConfig;

/**
 * Response model for STRAT pack API.
 *
 * <p>This is the public-facing representation of a STRAT pack,
 * containing only the information needed for purchase decisions.</p>
 */
public record StratPackResponse(@JsonProperty("packId") String packId, @JsonProperty("displayName") String displayName,
		@JsonProperty("description") String description, @JsonProperty("priceCents") Integer priceCents,
		@JsonProperty("priceFormatted") String priceFormatted, @JsonProperty("stratAmount") Long stratAmount,
		@JsonProperty("bonusStrat") Long bonusStrat, @JsonProperty("totalStrat") Long totalStrat,
		@JsonProperty("badge") String badge, @JsonProperty("pricePerStrat") Double pricePerStrat) {

	/**
	 * Create response from config entity.
	 */
	public static StratPackResponse fromConfig(StratPackConfig config) {
		int priceCents = config.getPriceCents() != null ? config.getPriceCents() : 0;
		String priceFormatted = String.format("$%.2f", priceCents / 100.0);

		return new StratPackResponse(config.getPackId(), config.getDisplayName(), config.getDescription(), priceCents,
				priceFormatted, config.getStratAmount(), config.getBonusStrat(), config.getTotalStrat(),
				config.getBadge(), config.getPricePerStrat());
	}

}
