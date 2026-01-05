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
 * Provides queries for managing job metadata, schedules, and enable/disable status.
 *
 * Used by DynamicJobSchedulerBusiness to read job schedules and by AdminJobController
 * to manage jobs from the admin console.
 */
@Repository
public interface JobDefinitionRepository extends JpaRepository<JobDefinitionEntity, String> {

    /**
     * Find all enabled jobs (ready to be scheduled).
     * Used by DynamicJobSchedulerBusiness on startup to initialize schedules.
     *
     * @return List of enabled job definitions
     */
    List<JobDefinitionEntity> findByEnabledTrue();

    /**
     * Find jobs by group (MARKETDATA or FUNDAMENTALS).
     * Used for filtering jobs in admin console.
     *
     * @param jobGroup The job group to filter by
     * @return List of jobs in the specified group
     */
    List<JobDefinitionEntity> findByJobGroup(@Param("jobGroup") String jobGroup);

    /**
     * Find a specific job by its ID.
     *
     * @param jobId The unique job identifier
     * @return Optional containing the job definition, or empty if not found
     */
    Optional<JobDefinitionEntity> findByJobId(@Param("jobId") String jobId);

    /**
     * Find all CRON-scheduled jobs that are enabled.
     * Used to filter only jobs that need dynamic scheduling.
     *
     * @return List of enabled scheduled jobs
     */
    @Query("SELECT j FROM JobDefinitionEntity j " +
           "WHERE j.enabled = true " +
           "AND j.scheduleType = 'CRON' " +
           "AND j.scheduleCron IS NOT NULL " +
           "ORDER BY j.jobGroup, j.jobId")
    List<JobDefinitionEntity> findScheduledJobs();

    /**
     * Find all manual-trigger jobs.
     * Used for admin console to show jobs that can only be triggered manually.
     *
     * @return List of manual jobs
     */
    @Query("SELECT j FROM JobDefinitionEntity j " +
           "WHERE j.scheduleType = 'MANUAL' " +
           "ORDER BY j.jobGroup, j.jobId")
    List<JobDefinitionEntity> findManualJobs();

    /**
     * Count jobs by enabled status.
     * Used for admin console dashboard statistics.
     *
     * @param enabled Whether to count enabled or disabled jobs
     * @return Count of jobs with the specified status
     */
    long countByEnabled(@Param("enabled") Boolean enabled);

    /**
     * Find all jobs ordered by group and ID.
     * Used for admin console job listing page.
     *
     * @return List of all job definitions
     */
    @Query("SELECT j FROM JobDefinitionEntity j " +
           "ORDER BY j.jobGroup, j.jobId")
    List<JobDefinitionEntity> findAllOrdered();
}
