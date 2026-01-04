package io.strategiz.data.marketdata.timescale.repository;

import io.strategiz.data.marketdata.timescale.entity.JobExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for job execution history.
 * Provides queries for tracking batch job performance and debugging failures.
 */
@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, String> {

    /**
     * Find executions by job name, ordered by start time descending (most recent first).
     * Supports pagination for efficient loading.
     *
     * @param jobName The job name to filter by
     * @param pageable Pagination parameters
     * @return Page of execution records
     */
    Page<JobExecutionEntity> findByJobNameOrderByStartTimeDesc(
        @Param("jobName") String jobName,
        Pageable pageable
    );

    /**
     * Find recent executions for a job within a time window.
     *
     * @param jobName The job name to filter by
     * @param since Only return executions after this timestamp
     * @return List of execution records
     */
    @Query("SELECT j FROM JobExecutionEntity j " +
           "WHERE j.jobName = :jobName " +
           "AND j.startTime >= :since " +
           "ORDER BY j.startTime DESC")
    List<JobExecutionEntity> findRecentExecutions(
        @Param("jobName") String jobName,
        @Param("since") Instant since
    );

    /**
     * Get execution statistics grouped by status for a job.
     * Returns array of [status, count] tuples.
     *
     * @param jobName The job name to filter by
     * @param since Only count executions after this timestamp
     * @return List of [status, count] arrays
     */
    @Query("SELECT j.status, COUNT(j) FROM JobExecutionEntity j " +
           "WHERE j.jobName = :jobName " +
           "AND j.startTime >= :since " +
           "GROUP BY j.status")
    List<Object[]> getExecutionStatsByJobName(
        @Param("jobName") String jobName,
        @Param("since") Instant since
    );

    /**
     * Calculate average duration for successful job executions.
     *
     * @param jobName The job name to filter by
     * @param since Only average executions after this timestamp
     * @return Average duration in milliseconds, or null if no successful executions
     */
    @Query("SELECT AVG(j.durationMs) FROM JobExecutionEntity j " +
           "WHERE j.jobName = :jobName " +
           "AND j.status = 'SUCCESS' " +
           "AND j.startTime >= :since")
    Long getAverageDuration(
        @Param("jobName") String jobName,
        @Param("since") Instant since
    );

    /**
     * Find the most recent execution for a job.
     *
     * @param jobName The job name to filter by
     * @return Most recent execution, or empty if none
     */
    @Query("SELECT j FROM JobExecutionEntity j " +
           "WHERE j.jobName = :jobName " +
           "ORDER BY j.startTime DESC " +
           "LIMIT 1")
    Optional<JobExecutionEntity> findLatestByJobName(@Param("jobName") String jobName);

    /**
     * Find executions by status.
     *
     * @param status The status to filter by (SUCCESS, FAILED, RUNNING)
     * @param pageable Pagination parameters
     * @return Page of execution records
     */
    Page<JobExecutionEntity> findByStatusOrderByStartTimeDesc(
        @Param("status") String status,
        Pageable pageable
    );

    /**
     * Count executions by job name and status within a time window.
     *
     * @param jobName The job name to filter by
     * @param status The status to filter by
     * @param since Only count executions after this timestamp
     * @return Count of matching executions
     */
    @Query("SELECT COUNT(j) FROM JobExecutionEntity j " +
           "WHERE j.jobName = :jobName " +
           "AND j.status = :status " +
           "AND j.startTime >= :since")
    Long countByJobNameAndStatusSince(
        @Param("jobName") String jobName,
        @Param("status") String status,
        @Param("since") Instant since
    );

    /**
     * Find all executions within a date range for any job.
     *
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (exclusive)
     * @param pageable Pagination parameters
     * @return Page of execution records
     */
    @Query("SELECT j FROM JobExecutionEntity j " +
           "WHERE j.startTime >= :startDate " +
           "AND j.startTime < :endDate " +
           "ORDER BY j.startTime DESC")
    Page<JobExecutionEntity> findByDateRange(
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    /**
     * Find all executions across all jobs, ordered by start time descending.
     * Used for admin console to view all job history.
     *
     * @param pageable Pagination parameters
     * @return Page of execution records
     */
    @Query("SELECT j FROM JobExecutionEntity j " +
           "ORDER BY j.startTime DESC")
    Page<JobExecutionEntity> findAllOrderByStartTimeDesc(Pageable pageable);

    /**
     * Find all executions across all jobs since a timestamp.
     * Used for calculating aggregate statistics.
     *
     * @param since Only return executions after this timestamp
     * @return List of execution records
     */
    @Query("SELECT j FROM JobExecutionEntity j " +
           "WHERE j.startTime >= :since " +
           "ORDER BY j.startTime DESC")
    List<JobExecutionEntity> findAllSince(@Param("since") Instant since);
}
