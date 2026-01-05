package io.strategiz.business.marketdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for dynamic job scheduling.
 *
 * Provides TaskScheduler bean used by DynamicJobSchedulerBusiness to
 * schedule jobs based on CRON expressions from database.
 *
 * Thread Pool Configuration:
 * - Pool size: 10 threads (handles multiple concurrent scheduled jobs)
 * - Thread name prefix: "job-scheduler-" (for debugging/monitoring)
 * - Daemon threads: false (keeps JVM alive if jobs are running)
 * - Await termination: true (graceful shutdown waits for jobs to complete)
 *
 * Note: @EnableScheduling is still needed for any remaining @Scheduled annotations
 * during the migration phase. Once all jobs are migrated to database-driven scheduling,
 * this can be removed if no other @Scheduled methods exist.
 */
@Configuration
@EnableScheduling
public class DynamicSchedulerConfig {

    /**
     * TaskScheduler bean for dynamic job scheduling.
     * Used by DynamicJobSchedulerBusiness to schedule jobs with CRON triggers.
     *
     * @return Configured ThreadPoolTaskScheduler
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // Thread pool configuration
        scheduler.setPoolSize(10);  // Support up to 10 concurrent scheduled jobs
        scheduler.setThreadNamePrefix("job-scheduler-");
        scheduler.setDaemon(false);  // Keep JVM alive for scheduled tasks

        // Graceful shutdown
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);  // Wait up to 30s for jobs to finish

        // Error handling
        scheduler.setErrorHandler(t -> {
            // Log uncaught exceptions in scheduled tasks
            // Individual job failures are handled in DynamicJobSchedulerBusiness.executeJob()
            org.slf4j.LoggerFactory.getLogger(DynamicSchedulerConfig.class)
                .error("Uncaught exception in scheduled task", t);
        });

        scheduler.initialize();
        return scheduler;
    }
}
