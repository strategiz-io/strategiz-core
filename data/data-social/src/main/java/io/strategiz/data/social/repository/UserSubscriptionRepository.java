package io.strategiz.data.social.repository;

/**
 * @deprecated Use {@link OwnerSubscriptionRepository} instead.
 * This interface is kept for backward compatibility only.
 *
 * <p>UserSubscriptionRepository has been renamed to OwnerSubscriptionRepository to clearly
 * distinguish it from platform subscription operations.</p>
 */
@Deprecated(forRemoval = true)
public interface UserSubscriptionRepository extends OwnerSubscriptionRepository {
    // All methods inherited from OwnerSubscriptionRepository
}
