package io.strategiz.service.console.logging;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Helper to provide ApplicationContext to Logback appenders.
 *
 * Logback initializes before Spring, so appenders cannot directly inject Spring beans.
 * This component implements ApplicationContextAware to capture the Spring context once
 * it's available, making it accessible to BatchJobLogAppender for lazy service loading.
 */
@Component
public class LogbackConfigurationHelper implements ApplicationContextAware {

	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext context) {
		applicationContext = context;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
