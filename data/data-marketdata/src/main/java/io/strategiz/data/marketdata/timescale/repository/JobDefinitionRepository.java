package io.strategiz.data.marketdata.timescale.repository;

import io.strategiz.data.marketdata.timescale.entity.JobDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for job definitions.
 * Provides queries for managing job schedules and configurations from the admin console.
 */
@Repository
public interface JobDefinitionRepository extends JpaRepository<JobDefinitionEntity, String> {

	/**
	 * Find all enabled jobs.
	 * Used for displaying active jobs in admin console.
	 *
	 * @return List of enabled job definitions
	 */
	List<JobDefinitionEntity> findByEnabledTrue();

	/**
	 * Find jobs by group (MARKETDATA or FUNDAMENTALS).
	 * Useful for organizing jobs by category in the admin console.
	 *
	 * @param jobGroup The job group to filter by
	 * @return List of job definitions in the group
	 */
	List<JobDefinitionEntity> findByJobGroup(@Param("jobGroup") String jobGroup);

	/**
	 * Find a specific job by its ID.
	 * Convenience method that returns Optional for safety.
	 *
	 * @param jobId The job ID to find
	 * @return Job definition if found, empty otherwise
	 */
	Optional<JobDefinitionEntity> findByJobId(@Param("jobId") String jobId);

	/**
	 * Find all scheduled jobs (CRON type) that are enabled.
	 * Used by DynamicJobSchedulerBusiness to initialize scheduled tasks on startup.
	 *
	 * @return List of enabled scheduled jobs
	 */
	@Query("SELECT j FROM JobDefinitionEntity j "
			+ "WHERE j.scheduleType = 'CRON' "
			+ "AND j.enabled = true "
			+ "AND j.scheduleCron IS NOT NULL")
	List<JobDefinitionEntity> findScheduledJobs();

	/**
	 * Find all manual jobs (triggered via console or API).
	 * Manual jobs have scheduleType = 'MANUAL' and no cron schedule.
	 *
	 * @return List of manual job definitions
	 */
	@Query("SELECT j FROM JobDefinitionEntity j " + "WHERE j.scheduleType = 'MANUAL' " + "ORDER BY j.displayName")
	List<JobDefinitionEntity> findManualJobs();

	/**
	 * Count enabled jobs by group.
	 * Used for dashboard statistics.
	 *
	 * @param jobGroup The job group to count
	 * @return Number of enabled jobs in the group
	 */
	@Query("SELECT COUNT(j) FROM JobDefinitionEntity j "
			+ "WHERE j.jobGroup = :jobGroup "
			+ "AND j.enabled = true")
	Long countEnabledByJobGroup(@Param("jobGroup") String jobGroup);

	/**
	 * Find all jobs ordered by group and display name.
	 * Used for admin console job listing.
	 *
	 * @return List of all job definitions, sorted
	 */
	@Query("SELECT j FROM JobDefinitionEntity j " + "ORDER BY j.jobGroup, j.displayName")
	List<JobDefinitionEntity> findAllOrderByGroupAndName();

}
