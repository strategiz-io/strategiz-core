package io.strategiz.business.marketdata;

import io.strategiz.data.marketdata.firestore.entity.JobDefinitionFirestoreEntity;
import io.strategiz.data.marketdata.firestore.repository.JobDefinitionFirestoreRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Dynamic job scheduler that reads job schedules from database instead of using @Scheduled
 * annotations. This allows schedules to be updated at runtime without code changes.
 *
 * Jobs are stored in the 'jobs' table in TimescaleDB with their schedules. On startup, this
 * service reads all enabled CRON jobs and schedules them. Admins can update schedules via REST API
 * and changes take effect immediately.
 */
@Service
public class DynamicJobSchedulerBusiness {

	private static final Logger log = LoggerFactory.getLogger(DynamicJobSchedulerBusiness.class);

	private final TaskScheduler taskScheduler;

	private final JobDefinitionFirestoreRepository jobDefinitionRepository;

	private final ApplicationContext applicationContext;

	// Track scheduled tasks so we can cancel/reschedule them
	private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

	public DynamicJobSchedulerBusiness(TaskScheduler taskScheduler, JobDefinitionFirestoreRepository jobDefinitionRepository,
			ApplicationContext applicationContext) {
		this.taskScheduler = taskScheduler;
		this.jobDefinitionRepository = jobDefinitionRepository;
		this.applicationContext = applicationContext;
	}

	/**
	 * Initialize job schedules from database on application startup. Only schedules enabled CRON
	 * jobs. Manual jobs are triggered via REST API.
	 */
	@PostConstruct
	public void initializeSchedules() {
		try {
			List<JobDefinitionFirestoreEntity> jobs = jobDefinitionRepository.findScheduledJobs();
			log.info("Initializing dynamic job scheduler with {} jobs from database", jobs.size());

			for (JobDefinitionFirestoreEntity job : jobs) {
				try {
					scheduleJob(job);
					log.info("Scheduled job: {} with cron: {}", job.getJobId(), job.getScheduleCron());
				}
				catch (Exception e) {
					log.error("Failed to schedule job {}: {}", job.getJobId(), e.getMessage(), e);
				}
			}

			log.info("Dynamic job scheduler initialized with {} active schedules", scheduledTasks.size());
		}
		catch (Exception e) {
			log.error("Failed to initialize dynamic job scheduler: {}. App will start without scheduled jobs.",
					e.getMessage(), e);
		}
	}

	/**
	 * Schedule a job with its cron expression. If already scheduled, cancels the old schedule
	 * first.
	 *
	 * @param job Job definition with schedule
	 */
	public void scheduleJob(JobDefinitionFirestoreEntity job) {
		// Cancel existing schedule if present
		cancelJob(job.getJobId());

		// Create new schedule
		CronTrigger trigger = new CronTrigger(job.getScheduleCron());
		ScheduledFuture<?> future = taskScheduler.schedule(() -> executeJob(job.getJobId()), trigger);

		scheduledTasks.put(job.getJobId(), future);
		log.info("Scheduled job {} with cron: {}", job.getJobId(), job.getScheduleCron());
	}

	/**
	 * Update the schedule for a job. Cancels old schedule and creates new one.
	 *
	 * @param jobId Job ID
	 * @param newCron New cron expression
	 */
	public void updateSchedule(String jobId, String newCron) {
		// Get job definition
		JobDefinitionFirestoreEntity job = jobDefinitionRepository.findById(jobId)
			.orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

		// Update in database
		job.setScheduleCron(newCron);
		jobDefinitionRepository.save(job);

		// Reschedule
		scheduleJob(job);
		log.info("Job {} rescheduled with new cron: {}", jobId, newCron);
	}

	/**
	 * Cancel a scheduled job.
	 *
	 * @param jobId Job ID
	 */
	public void cancelJob(String jobId) {
		ScheduledFuture<?> future = scheduledTasks.remove(jobId);
		if (future != null) {
			future.cancel(false); // Don't interrupt if running
			log.info("Canceled scheduled job: {}", jobId);
		}
	}

	/**
	 * Execute a job by invoking its execute() method via reflection. This is called by the
	 * scheduler when the cron trigger fires.
	 *
	 * @param jobId Job ID (e.g., MARKETDATA_BACKFILL)
	 */
	public void executeJob(String jobId) {
		log.info("Executing job: {}", jobId);

		try {
			// Get job definition
			JobDefinitionFirestoreEntity jobDef = jobDefinitionRepository.findById(jobId)
				.orElseThrow(() -> new IllegalStateException("Job definition not found: " + jobId));

			// Get job bean from Spring context
			Class<?> jobClass = Class.forName(jobDef.getJobClass());
			Object jobBean = applicationContext.getBean(jobClass);

			// Invoke execute() method
			Method executeMethod = jobBean.getClass().getMethod("execute");
			executeMethod.invoke(jobBean);

			log.info("Job {} completed successfully", jobId);
		}
		catch (Exception e) {
			log.error("Failed to execute job {}: {}", jobId, e.getMessage(), e);
		}
	}

	/**
	 * Shutdown hook to cancel all scheduled tasks gracefully.
	 */
	@PreDestroy
	public void shutdown() {
		log.info("Shutting down dynamic job scheduler, cancelling {} tasks", scheduledTasks.size());
		for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
			entry.getValue().cancel(false);
			log.debug("Cancelled job: {}", entry.getKey());
		}
		scheduledTasks.clear();
	}

}
