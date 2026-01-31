package io.strategiz.data.social.entity;

/**
 * @deprecated Use {@link OwnerSubscription} instead. This class is kept for backward
 * compatibility only.
 *
 * <p>
 * UserSubscription has been renamed to OwnerSubscription to clearly distinguish it from
 * PlatformSubscription (in data-preferences) which represents user-to-platform
 * subscriptions.
 * </p>
 *
 * <p>
 * Additionally, the tier field has been removed. Owner subscriptions now have a single
 * price set by the owner - no tiers (FREE/BASIC/PREMIUM).
 * </p>
 */
@Deprecated(forRemoval = true)
public class UserSubscription extends OwnerSubscription {

	public UserSubscription() {
		super();
	}

}
