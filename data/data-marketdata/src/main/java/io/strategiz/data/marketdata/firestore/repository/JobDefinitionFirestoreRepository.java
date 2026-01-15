package io.strategiz.data.marketdata.firestore.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import io.strategiz.data.marketdata.firestore.entity.JobDefinitionFirestoreEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore repository for job definitions. Stores job metadata and schedules. Replaces
 * TimescaleDB JobDefinitionRepository.
 *
 * Collection: batch_jobs
 */
@Repository
public class JobDefinitionFirestoreRepository {

	private static final Logger log = LoggerFactory.getLogger(JobDefinitionFirestoreRepository.class);

	private static final String COLLECTION_NAME = "batch_jobs";

	private final Firestore firestore;

	public JobDefinitionFirestoreRepository(Firestore firestore) {
		this.firestore = firestore;
	}

	private CollectionReference getCollection() {
		return firestore.collection(COLLECTION_NAME);
	}

	/**
	 * Save a job definition.
	 */
	public JobDefinitionFirestoreEntity save(JobDefinitionFirestoreEntity entity) {
		try {
			if (entity.getJobId() == null || entity.getJobId().isEmpty()) {
				throw new IllegalArgumentException("Job ID is required");
			}

			entity.setUpdatedAt(Instant.now());
			if (entity.getCreatedAt() == null) {
				entity.setCreatedAt(Instant.now());
			}

			getCollection().document(entity.getJobId()).set(entity).get();
			log.debug("Saved job definition: {}", entity.getJobId());
			return entity;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while saving job definition", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to save job definition", e);
		}
	}

	/**
	 * Find job by ID.
	 */
	public Optional<JobDefinitionFirestoreEntity> findById(String jobId) {
		try {
			DocumentSnapshot doc = getCollection().document(jobId).get().get();
			if (doc.exists()) {
				JobDefinitionFirestoreEntity entity = doc.toObject(JobDefinitionFirestoreEntity.class);
				if (entity != null) {
					entity.setJobId(doc.getId());
					return Optional.of(entity);
				}
			}
			return Optional.empty();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while finding job definition", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to find job definition", e);
		}
	}

	/**
	 * Alias for findById for API compatibility.
	 */
	public Optional<JobDefinitionFirestoreEntity> findByJobId(String jobId) {
		return findById(jobId);
	}

	/**
	 * Find all job definitions.
	 */
	public List<JobDefinitionFirestoreEntity> findAll() {
		try {
			List<QueryDocumentSnapshot> docs = getCollection().get().get().getDocuments();
			return docs.stream().map(doc -> {
				JobDefinitionFirestoreEntity entity = doc.toObject(JobDefinitionFirestoreEntity.class);
				entity.setJobId(doc.getId());
				return entity;
			}).collect(Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while finding all job definitions", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to find all job definitions", e);
		}
	}

	/**
	 * Find all enabled jobs.
	 */
	public List<JobDefinitionFirestoreEntity> findByEnabledTrue() {
		try {
			Query query = getCollection().whereEqualTo("enabled", true);
			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
			return docs.stream().map(doc -> {
				JobDefinitionFirestoreEntity entity = doc.toObject(JobDefinitionFirestoreEntity.class);
				entity.setJobId(doc.getId());
				return entity;
			}).collect(Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while finding enabled jobs", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to find enabled jobs", e);
		}
	}

	/**
	 * Find jobs by group.
	 */
	public List<JobDefinitionFirestoreEntity> findByJobGroup(String jobGroup) {
		try {
			Query query = getCollection().whereEqualTo("jobGroup", jobGroup);
			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
			return docs.stream().map(doc -> {
				JobDefinitionFirestoreEntity entity = doc.toObject(JobDefinitionFirestoreEntity.class);
				entity.setJobId(doc.getId());
				return entity;
			}).collect(Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while finding jobs by group", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to find jobs by group", e);
		}
	}

	/**
	 * Find all scheduled (CRON) jobs that are enabled.
	 */
	public List<JobDefinitionFirestoreEntity> findScheduledJobs() {
		try {
			Query query = getCollection().whereEqualTo("scheduleType", "CRON").whereEqualTo("enabled", true);
			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
			return docs.stream().filter(doc -> doc.getString("scheduleCron") != null).map(doc -> {
				JobDefinitionFirestoreEntity entity = doc.toObject(JobDefinitionFirestoreEntity.class);
				entity.setJobId(doc.getId());
				return entity;
			}).collect(Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while finding scheduled jobs", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to find scheduled jobs", e);
		}
	}

	/**
	 * Find all manual jobs.
	 */
	public List<JobDefinitionFirestoreEntity> findManualJobs() {
		try {
			Query query = getCollection().whereEqualTo("scheduleType", "MANUAL");
			List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
			return docs.stream().map(doc -> {
				JobDefinitionFirestoreEntity entity = doc.toObject(JobDefinitionFirestoreEntity.class);
				entity.setJobId(doc.getId());
				return entity;
			})
				.sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
				.collect(Collectors.toList());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while finding manual jobs", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to find manual jobs", e);
		}
	}

	/**
	 * Count enabled jobs by group.
	 */
	public Long countEnabledByJobGroup(String jobGroup) {
		try {
			Query query = getCollection().whereEqualTo("jobGroup", jobGroup).whereEqualTo("enabled", true);
			return (long) query.get().get().size();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while counting jobs", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to count jobs", e);
		}
	}

	/**
	 * Find all jobs ordered by group and name.
	 */
	public List<JobDefinitionFirestoreEntity> findAllOrderByGroupAndName() {
		return findAll().stream().sorted((a, b) -> {
			int groupCompare = (a.getJobGroup() != null ? a.getJobGroup() : "")
				.compareToIgnoreCase(b.getJobGroup() != null ? b.getJobGroup() : "");
			if (groupCompare != 0) {
				return groupCompare;
			}
			return (a.getDisplayName() != null ? a.getDisplayName() : "")
				.compareToIgnoreCase(b.getDisplayName() != null ? b.getDisplayName() : "");
		}).collect(Collectors.toList());
	}

	/**
	 * Delete a job definition.
	 */
	public void deleteById(String jobId) {
		try {
			getCollection().document(jobId).delete().get();
			log.debug("Deleted job definition: {}", jobId);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while deleting job definition", e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to delete job definition", e);
		}
	}

	/**
	 * Check if job exists.
	 */
	public boolean existsById(String jobId) {
		return findById(jobId).isPresent();
	}

}
