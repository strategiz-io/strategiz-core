package io.strategiz.service.cryptotoken.model;

/**
 * Response DTO for current conversion rates.
 */
public record ConversionRatesResponse(long tokensPerUsd, // $1 = 100 STRAT
		double platformFeePercent, // 15%
		long aiCreditsPerToken, // 1 STRAT = X AI credits
		long minimumPurchaseCents // Minimum USD purchase in cents
) {

	public static ConversionRatesResponse getDefault() {
		return new ConversionRatesResponse(100, // $1 = 100 STRAT
				0.15, // 15% platform fee
				10, // 1 STRAT = 10 AI credits
				500 // Minimum $5 purchase
		);
	}

}
