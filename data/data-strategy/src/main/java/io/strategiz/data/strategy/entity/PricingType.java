package io.strategiz.data.strategy.entity;

/**
 * Pricing model types for strategies.
 */
public enum PricingType {

	/**
	 * Strategy is free to use - no payment required.
	 */
	FREE,

	/**
	 * Single one-time purchase for lifetime access.
	 */
	ONE_TIME,

	/**
	 * Monthly recurring subscription.
	 */
	SUBSCRIPTION

}
