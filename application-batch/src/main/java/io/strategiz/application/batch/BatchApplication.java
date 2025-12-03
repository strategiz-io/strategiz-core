package io.strategiz.application.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Batch Application - Runs scheduled jobs on port 8444
 *
 * This application runs with the "scheduler" profile active
 * and handles all batch processing tasks including:
 * - Market data collection jobs
 * - Alpaca intraday data collection
 * - Other scheduled batch operations
 *
 * Unlike application-web (port 8443), this module:
 * - Does NOT expose REST APIs
 * - Only runs scheduled jobs
 * - Runs with @Profile("scheduler") annotations active
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "io.strategiz.application.batch",
    "io.strategiz.batch",
    "io.strategiz.business",
    "io.strategiz.data",
    "io.strategiz.client",
    "io.strategiz.framework"
})
public class BatchApplication {

    private static final Logger log = LoggerFactory.getLogger(BatchApplication.class);

    public static void main(String[] args) {
        log.info("===========================================");
        log.info("Starting Strategiz Batch Application");
        log.info("Profile: scheduler");
        log.info("Port: 8444");
        log.info("===========================================");

        System.setProperty("spring.profiles.active", "scheduler");
        SpringApplication.run(BatchApplication.class, args);
    }
}
