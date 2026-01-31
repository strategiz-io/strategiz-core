package io.strategiz.data.user.repository;

import io.strategiz.data.user.entity.EmailReservationEntity;

import java.util.Optional;

/**
 * Repository for email reservations in the userEmails collection.
 *
 * This repository leverages Firestore's document ID uniqueness to guarantee email
 * uniqueness across the platform. The document ID is the normalized lowercase email
 * address.
 *
 * Key behaviors: - reserve() will fail if the email document already exists (email taken)
 * - Transactions are supported via FirestoreTransactionHolder for atomic operations -
 * Expired PENDING reservations should be periodically cleaned up
 */
public interface EmailReservationRepository {

	/**
	 * Create a new email reservation. Uses Firestore create semantics - fails if document
	 * already exists.
	 * @param reservation The reservation to create
	 * @return The created reservation
	 * @throws io.strategiz.data.base.exception.DataRepositoryException if email already
	 * reserved
	 */
	EmailReservationEntity reserve(EmailReservationEntity reservation);

	/**
	 * Find a reservation by email address.
	 * @param email The normalized lowercase email address
	 * @return Optional containing the reservation if found
	 */
	Optional<EmailReservationEntity> findByEmail(String email);

	/**
	 * Check if an email is available for registration. An email is available if: 1. No
	 * reservation exists, OR 2. A PENDING reservation exists but has expired
	 *
	 * Note: This is a point-in-time check. For atomic operations, use reserve() within a
	 * transaction.
	 * @param email The email to check
	 * @return true if email is available, false otherwise
	 */
	boolean isEmailAvailable(String email);

	/**
	 * Confirm a pending reservation, transitioning it to CONFIRMED status. Should be
	 * called within a transaction along with user creation.
	 * @param email The email address to confirm
	 * @return The confirmed reservation
	 * @throws io.strategiz.data.base.exception.DataRepositoryException if reservation not
	 * found
	 */
	EmailReservationEntity confirm(String email);

	/**
	 * Create a CONFIRMED email reservation in a single write (no read). Used when the
	 * email was validated before the transaction (e.g., after OTP verification).
	 * @param reservation The reservation to create (will be set to CONFIRMED)
	 * @return The created reservation
	 */
	EmailReservationEntity createConfirmed(EmailReservationEntity reservation);

	/**
	 * Delete a reservation (e.g., when signup fails or is cancelled).
	 * @param email The email address to release
	 */
	void delete(String email);

	/**
	 * Delete all expired PENDING reservations. Should be called periodically by a
	 * scheduled job.
	 * @return Number of expired reservations deleted
	 */
	int deleteExpiredPendingReservations();

	/**
	 * Check if a reservation exists and is valid (not expired if PENDING).
	 * @param email The email to check
	 * @return true if a valid reservation exists
	 */
	boolean existsAndValid(String email);

}
