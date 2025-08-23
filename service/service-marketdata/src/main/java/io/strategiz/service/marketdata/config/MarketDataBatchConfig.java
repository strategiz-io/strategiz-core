package io.strategiz.service.marketdata.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Configuration for market data batch processing
 * Enables scheduling for daily batch jobs
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "polygon.batch.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataBatchConfig implements SchedulingConfigurer {
    
    /**
     * Configure the thread pool for scheduled tasks
     * Uses a dedicated thread pool to avoid blocking other operations
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2); // Small pool since we only run once per day
        scheduler.setThreadNamePrefix("marketdata-batch-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}