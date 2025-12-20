package io.strategiz.application.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Strategiz Console - Admin & Operations Hub
 *
 * This application serves as the central operations console for Strategiz platform:
 * - Scheduled batch jobs (market data collection, cleanup tasks)
 * - Admin operations (user management, system config)
 * - Observability dashboard integration
 * - System health monitoring
 *
 * Runs on port 8444 with the "scheduler" profile active.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
	"io.strategiz.application.console",
	"io.strategiz.batch",
	"io.strategiz.business",
	"io.strategiz.data",
	"io.strategiz.client",
	"io.strategiz.framework"
})
public class ConsoleApplication {

	private static final Logger log = LoggerFactory.getLogger(ConsoleApplication.class);

	public static void main(String[] args) {
		log.info("===========================================");
		log.info("  _____ _             _             _      ");
		log.info(" / ____| |           | |           (_)     ");
		log.info("| (___ | |_ _ __ __ _| |_ ___  __ _ _ ____");
		log.info(" \\___ \\| __| '__/ _` | __/ _ \\/ _` | |_  /");
		log.info(" ____) | |_| | | (_| | ||  __/ (_| | |/ / ");
		log.info("|_____/ \\__|_|  \\__,_|\\__\\___|\\__, |_/___|");
		log.info("                              __/ |       ");
		log.info("             CONSOLE         |___/        ");
		log.info("===========================================");
		log.info("Starting Strategiz Console - Operations Hub");
		log.info("Profile: scheduler");
		log.info("Port: 8444");
		log.info("===========================================");

		System.setProperty("spring.profiles.active", "scheduler");
		SpringApplication.run(ConsoleApplication.class, args);
	}

}
