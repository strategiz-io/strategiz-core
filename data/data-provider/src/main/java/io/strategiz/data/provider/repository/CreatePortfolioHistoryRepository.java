package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioHistoryEntity;

/**
 * Repository interface for creating portfolio history entities in Firestore.
 */
public interface CreatePortfolioHistoryRepository {

	/**
	 * Create a new portfolio history snapshot.
	 * @param snapshot The history entity to create
	 * @return The created snapshot with generated ID
	 */
	PortfolioHistoryEntity createSnapshot(PortfolioHistoryEntity snapshot);

	/**
	 * Create or update a snapshot (upsert operation).
	 * @param snapshot The history entity to save
	 * @return The saved snapshot
	 */
	PortfolioHistoryEntity saveSnapshot(PortfolioHistoryEntity snapshot);

}
