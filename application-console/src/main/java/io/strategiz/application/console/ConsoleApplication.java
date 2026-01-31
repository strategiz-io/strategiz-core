package io.strategiz.application.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Console Application - Admin REST API and Batch Jobs service.
 *
 * Serves admin-only endpoints at /v1/console/* and runs batch jobs. Separate deployment
 * from main API for independent scaling and updates. Batch jobs can run without being
 * killed by main API deployments.
 *
 * Endpoints served: - /v1/console/jobs - Job management - /v1/console/costs -
 * Infrastructure costs - /v1/console/users - User management - /v1/console/providers -
 * Provider status - /v1/console/observability - System metrics - /v1/console/quality -
 * Code quality metrics - /v1/marketdata/admin/* - Market data batch operations
 */
@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
		org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class })
@EnableCaching
@EnableScheduling
@ComponentScan(basePackages = { "io.strategiz" })
public class ConsoleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsoleApplication.class, args);
	}

}
