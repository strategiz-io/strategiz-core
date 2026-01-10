package io.strategiz.data.marketdata.clickhouse.repository;

import io.strategiz.data.marketdata.entity.JobExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ClickHouse implementation of job execution repository. Tracks batch job execution
 * history for monitoring and debugging.
 */
@Repository
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class JobExecutionClickHouseRepository {

	private static final Logger log = LoggerFactory.getLogger(JobExecutionClickHouseRepository.class);

	private final JdbcTemplate jdbcTemplate;

	private final JobExecutionRowMapper rowMapper = new JobExecutionRowMapper();

	public JobExecutionClickHouseRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Save a new job execution record.
	 */
	public JobExecutionEntity save(JobExecutionEntity entity) {
		if (entity.getExecutionId() == null) {
			entity.setExecutionId(UUID.randomUUID().toString());
		}

		String sql = """
				INSERT INTO job_executions (
				    execution_id, job_name, job_id, status, start_time, end_time, duration_ms,
				    symbols_processed, data_points_stored, error_count, error_details, timeframes, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		jdbcTemplate.update(sql, entity.getExecutionId(), entity.getJobName(), entity.getJobId(), entity.getStatus(),
				Timestamp.from(entity.getStartTime()),
				entity.getEndTime() != null ? Timestamp.from(entity.getEndTime()) : null, entity.getDurationMs(),
				entity.getSymbolsProcessed(), entity.getDataPointsStored(), entity.getErrorCount(),
				entity.getErrorDetails(), entity.getTimeframes(),
				entity.getCreatedAt() != null ? Timestamp.from(entity.getCreatedAt()) : Timestamp.from(Instant.now()));

		return entity;
	}

	/**
	 * Find execution by ID.
	 */
	public Optional<JobExecutionEntity> findById(String executionId) {
		String sql = "SELECT * FROM job_executions WHERE execution_id = ?";
		List<JobExecutionEntity> results = jdbcTemplate.query(sql, rowMapper, executionId);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	/**
	 * Find executions by job name with pagination.
	 */
	public Page<JobExecutionEntity> findByJobNameOrderByStartTimeDesc(String jobName, Pageable pageable) {
		String countSql = "SELECT COUNT(*) FROM job_executions WHERE job_name = ?";
		Long total = jdbcTemplate.queryForObject(countSql, Long.class, jobName);

		String sql = """
				SELECT * FROM job_executions
				WHERE job_name = ?
				ORDER BY start_time DESC
				LIMIT ? OFFSET ?
				""";
		List<JobExecutionEntity> results = jdbcTemplate.query(sql, rowMapper, jobName, pageable.getPageSize(),
				pageable.getOffset());

		return new PageImpl<>(results, pageable, total != null ? total : 0);
	}

	/**
	 * Find recent executions for a job since a timestamp.
	 */
	public List<JobExecutionEntity> findRecentExecutions(String jobName, Instant since) {
		String sql = """
				SELECT * FROM job_executions
				WHERE job_name = ? AND start_time >= ?
				ORDER BY start_time DESC
				""";
		return jdbcTemplate.query(sql, rowMapper, jobName, Timestamp.from(since));
	}

	/**
	 * Find the most recent execution for a job.
	 */
	public Optional<JobExecutionEntity> findLatestByJobName(String jobName) {
		String sql = """
				SELECT * FROM job_executions
				WHERE job_name = ?
				ORDER BY start_time DESC
				LIMIT 1
				""";
		List<JobExecutionEntity> results = jdbcTemplate.query(sql, rowMapper, jobName);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	/**
	 * Find executions by status with pagination.
	 */
	public Page<JobExecutionEntity> findByStatusOrderByStartTimeDesc(String status, Pageable pageable) {
		String countSql = "SELECT COUNT(*) FROM job_executions WHERE status = ?";
		Long total = jdbcTemplate.queryForObject(countSql, Long.class, status);

		String sql = """
				SELECT * FROM job_executions
				WHERE status = ?
				ORDER BY start_time DESC
				LIMIT ? OFFSET ?
				""";
		List<JobExecutionEntity> results = jdbcTemplate.query(sql, rowMapper, status, pageable.getPageSize(),
				pageable.getOffset());

		return new PageImpl<>(results, pageable, total != null ? total : 0);
	}

	/**
	 * Find all executions with pagination.
	 */
	public Page<JobExecutionEntity> findAllOrderByStartTimeDesc(Pageable pageable) {
		String countSql = "SELECT COUNT(*) FROM job_executions";
		Long total = jdbcTemplate.queryForObject(countSql, Long.class);

		String sql = """
				SELECT * FROM job_executions
				ORDER BY start_time DESC
				LIMIT ? OFFSET ?
				""";
		List<JobExecutionEntity> results = jdbcTemplate.query(sql, rowMapper, pageable.getPageSize(),
				pageable.getOffset());

		return new PageImpl<>(results, pageable, total != null ? total : 0);
	}

	/**
	 * Find all executions since a timestamp.
	 */
	public List<JobExecutionEntity> findAllSince(Instant since) {
		String sql = """
				SELECT * FROM job_executions
				WHERE start_time >= parseDateTimeBestEffort(?)
				ORDER BY start_time DESC
				""";
		return jdbcTemplate.query(sql, rowMapper, since.toString());
	}

	/**
	 * Find stale RUNNING jobs.
	 */
	public List<JobExecutionEntity> findStaleRunningJobs(Instant before) {
		String sql = """
				SELECT * FROM job_executions
				WHERE status = 'RUNNING' AND start_time < parseDateTimeBestEffort(?)
				""";
		return jdbcTemplate.query(sql, rowMapper, before.toString());
	}

	/**
	 * Get execution statistics grouped by status.
	 */
	public List<Object[]> getExecutionStatsByJobName(String jobName, Instant since) {
		String sql = """
				SELECT status, COUNT(*) as count FROM job_executions
				WHERE job_name = ? AND start_time >= ?
				GROUP BY status
				""";
		return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[] { rs.getString("status"), rs.getLong("count") },
				jobName, Timestamp.from(since));
	}

	/**
	 * Get average duration for successful executions.
	 */
	public Long getAverageDuration(String jobName, Instant since) {
		String sql = """
				SELECT AVG(duration_ms) FROM job_executions
				WHERE job_name = ? AND status = 'SUCCESS' AND start_time >= ?
				""";
		return jdbcTemplate.queryForObject(sql, Long.class, jobName, Timestamp.from(since));
	}

	/**
	 * Count executions by job name and status since a timestamp.
	 */
	public Long countByJobNameAndStatusSince(String jobName, String status, Instant since) {
		String sql = """
				SELECT COUNT(*) FROM job_executions
				WHERE job_name = ? AND status = ? AND start_time >= ?
				""";
		return jdbcTemplate.queryForObject(sql, Long.class, jobName, status, Timestamp.from(since));
	}

	/**
	 * Find executions by date range.
	 */
	public Page<JobExecutionEntity> findByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
		String countSql = "SELECT COUNT(*) FROM job_executions WHERE start_time >= ? AND start_time < ?";
		Long total = jdbcTemplate.queryForObject(countSql, Long.class, Timestamp.from(startDate),
				Timestamp.from(endDate));

		String sql = """
				SELECT * FROM job_executions
				WHERE start_time >= ? AND start_time < ?
				ORDER BY start_time DESC
				LIMIT ? OFFSET ?
				""";
		List<JobExecutionEntity> results = jdbcTemplate.query(sql, rowMapper, Timestamp.from(startDate),
				Timestamp.from(endDate), pageable.getPageSize(), pageable.getOffset());

		return new PageImpl<>(results, pageable, total != null ? total : 0);
	}

	/**
	 * Row mapper for JobExecutionEntity.
	 */
	private static class JobExecutionRowMapper implements RowMapper<JobExecutionEntity> {

		@Override
		public JobExecutionEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			JobExecutionEntity entity = new JobExecutionEntity();
			entity.setExecutionId(rs.getString("execution_id"));
			entity.setJobName(rs.getString("job_name"));
			entity.setJobId(rs.getString("job_id"));
			entity.setStatus(rs.getString("status"));
			entity.setStartTime(rs.getTimestamp("start_time").toInstant());

			Timestamp endTime = rs.getTimestamp("end_time");
			if (endTime != null) {
				entity.setEndTime(endTime.toInstant());
			}

			entity.setDurationMs(rs.getLong("duration_ms"));
			entity.setSymbolsProcessed(rs.getInt("symbols_processed"));
			entity.setDataPointsStored(rs.getLong("data_points_stored"));
			entity.setErrorCount(rs.getInt("error_count"));
			entity.setErrorDetails(rs.getString("error_details"));
			entity.setTimeframes(rs.getString("timeframes"));

			Timestamp createdAt = rs.getTimestamp("created_at");
			if (createdAt != null) {
				entity.setCreatedAt(createdAt.toInstant());
			}

			return entity;
		}

	}

}
