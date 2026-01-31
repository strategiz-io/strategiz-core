package io.strategiz.data.preferences.entity;

/**
 * @deprecated Use {@link PlatformSubscription} instead. This class is kept for backward
 * compatibility only.
 *
 * <p>
 * UserSubscription has been renamed to PlatformSubscription to clearly distinguish it
 * from OwnerSubscription (in data-social) which represents user-to-user subscriptions.
 * </p>
 */
@Deprecated(forRemoval = true)
public class UserSubscription extends PlatformSubscription {

	public UserSubscription() {
		super();
	}

	public UserSubscription(SubscriptionTier tier) {
		super(tier);
	}

}
