package io.strategiz.data.waitlist.repository;

import io.strategiz.data.waitlist.entity.WaitlistEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for waitlist collection - manages pre-launch email signups
 *
 * This repository handles: 1. CRUD operations for waitlist entries 2. Duplicate detection
 * via email hash 3. Counting and listing operations for admin
 */
public interface WaitlistRepository {

	/**
	 * Find waitlist entry by email hash (for duplicate detection)
	 */
	Optional<WaitlistEntity> findByEmailHash(String emailHash);

	/**
	 * Save waitlist entry (create or update)
	 */
	WaitlistEntity save(WaitlistEntity entity);

	/**
	 * Find all waitlist entries (admin function)
	 */
	List<WaitlistEntity> findAll();

	/**
	 * Count total waitlist entries
	 */
	long count();

	/**
	 * Find unconfirmed waitlist entries (for follow-up emails)
	 */
	List<WaitlistEntity> findUnconfirmed();

	/**
	 * Find waitlist entry by ID
	 */
	Optional<WaitlistEntity> findById(String id);

	/**
	 * Delete waitlist entry by ID
	 */
	void deleteById(String id);

}
