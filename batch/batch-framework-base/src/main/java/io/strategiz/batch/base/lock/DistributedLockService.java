package io.strategiz.batch.base.lock;

import io.strategiz.batch.base.exception.BatchErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Service for distributed locking across batch job instances.
 *
 * Default implementation uses in-memory locks (for single-instance deployments).
 * For multi-instance deployments, inject a Firestore or Redis-backed implementation.
 *
 * Lock storage backends:
 * - In-memory (default): ConcurrentHashMap for single-instance
 * - Firestore: FirestoreDistributedLockService (recommended for Cloud Run)
 * - Redis: RedisDistributedLockService (for high-throughput scenarios)
 */
public class DistributedLockService {

	private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

	private final ConcurrentHashMap<String, LockInfo> locks = new ConcurrentHashMap<>();

	private final String instanceId;

	public DistributedLockService() {
		this.instanceId = generateInstanceId();
		log.info("DistributedLockService initialized with instanceId: {}", instanceId);
	}

	/**
	 * Acquire a lock with the given name.
	 * @param lockName Unique lock identifier
	 * @param lockAtLeast Minimum time to hold lock
	 * @param lockAtMost Maximum time to hold lock (auto-release)
	 * @return Lock handle if acquired, empty if lock is held
	 */
	public Optional<LockHandle> tryAcquire(String lockName, Duration lockAtLeast, Duration lockAtMost) {
		Instant now = Instant.now();

		LockInfo existing = locks.get(lockName);
		if (existing != null && !existing.isExpired(now)) {
			log.debug("Lock '{}' already held by {}, expires at {}", lockName, existing.holderId, existing.expiresAt);
			return Optional.empty();
		}

		// Try to acquire
		LockInfo newLock = new LockInfo(instanceId, now, now.plus(lockAtMost), lockAtLeast);

		LockInfo previous = locks.putIfAbsent(lockName, newLock);
		if (previous != null && !previous.isExpired(now)) {
			// Lost race
			return Optional.empty();
		}

		// Acquired (or replaced expired lock)
		locks.put(lockName, newLock);
		log.info("Acquired lock '{}' until {}", lockName, newLock.expiresAt);

		return Optional.of(new LockHandle(lockName, this));
	}

	/**
	 * Release a lock.
	 */
	public void release(String lockName) {
		LockInfo lock = locks.get(lockName);
		if (lock == null) {
			log.warn("Attempted to release non-existent lock: {}", lockName);
			return;
		}

		if (!lock.holderId.equals(instanceId)) {
			log.warn("Attempted to release lock '{}' held by different instance: {}", lockName, lock.holderId);
			return;
		}

		// Check minimum hold time
		Instant now = Instant.now();
		Instant minReleaseTime = lock.acquiredAt.plus(lock.lockAtLeast);
		if (now.isBefore(minReleaseTime)) {
			log.debug("Lock '{}' cannot be released yet, minimum hold time not reached", lockName);
			// Don't remove, let it expire naturally
			return;
		}

		locks.remove(lockName);
		log.info("Released lock '{}'", lockName);
	}

	/**
	 * Execute operation with distributed lock.
	 * @throws StrategizException if lock cannot be acquired
	 */
	public <T> T executeWithLock(String lockName, Duration lockAtLeast, Duration lockAtMost, Supplier<T> operation) {
		Optional<LockHandle> handle = tryAcquire(lockName, lockAtLeast, lockAtMost);
		if (handle.isEmpty()) {
			throw new StrategizException(BatchErrorDetails.LOCK_ALREADY_HELD);
		}

		try {
			return operation.get();
		}
		finally {
			handle.get().release();
		}
	}

	/**
	 * Check if a lock is currently held.
	 */
	public boolean isLocked(String lockName) {
		LockInfo lock = locks.get(lockName);
		return lock != null && !lock.isExpired(Instant.now());
	}

	private String generateInstanceId() {
		// Use hostname + random suffix for uniqueness
		String hostname = System.getenv().getOrDefault("HOSTNAME", "local");
		return hostname + "-" + System.currentTimeMillis() % 10000;
	}

	/**
	 * Internal lock info.
	 */
	private record LockInfo(String holderId, Instant acquiredAt, Instant expiresAt, Duration lockAtLeast) {

		boolean isExpired(Instant now) {
			return now.isAfter(expiresAt);
		}
	}

	/**
	 * Handle for releasing an acquired lock.
	 */
	public static class LockHandle implements AutoCloseable {

		private final String lockName;

		private final DistributedLockService service;

		private boolean released = false;

		LockHandle(String lockName, DistributedLockService service) {
			this.lockName = lockName;
			this.service = service;
		}

		public void release() {
			if (!released) {
				service.release(lockName);
				released = true;
			}
		}

		@Override
		public void close() {
			release();
		}

	}

}
