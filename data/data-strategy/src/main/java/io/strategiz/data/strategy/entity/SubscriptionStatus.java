package io.strategiz.data.strategy.entity;

/**
 * Status of a strategy subscription.
 */
public enum SubscriptionStatus {

	/**
	 * Subscription is currently active and valid.
	 */
	ACTIVE,

	/**
	 * User is in a free trial period.
	 */
	TRIAL,

	/**
	 * User cancelled but subscription is still valid until expiry date.
	 */
	CANCELLED,

	/**
	 * Subscription period has ended.
	 */
	EXPIRED,

	/**
	 * Subscription is suspended (payment failed or admin action).
	 */
	SUSPENDED

}
