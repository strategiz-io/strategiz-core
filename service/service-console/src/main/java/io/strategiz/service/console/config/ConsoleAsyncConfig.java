package io.strategiz.service.console.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for console job execution. Enables asynchronous execution of
 * long-running jobs triggered from the console.
 */
@Configuration
@EnableAsync
public class ConsoleAsyncConfig {

	/**
	 * Task executor for console job execution. Uses a limited thread pool to prevent
	 * overwhelming the system with concurrent jobs.
	 */
	@Bean(name = "consoleTaskExecutor")
	public Executor consoleTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(10);
		executor.setThreadNamePrefix("console-job-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.initialize();
		return executor;
	}

}
