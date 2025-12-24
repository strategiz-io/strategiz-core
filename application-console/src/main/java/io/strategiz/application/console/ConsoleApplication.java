package io.strategiz.application.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

/**
 * Console Application - Admin REST API service
 *
 * Serves admin-only endpoints at /v1/console/*
 * Separate deployment from main API for independent scaling and updates
 */
@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = {
	"io.strategiz.application.console",
	"io.strategiz.service.console",
	"io.strategiz.batch.marketdata",
	"io.strategiz.business.tokenauth",
	"io.strategiz.data.provider",
	"io.strategiz.framework.authorization",
	"io.strategiz.framework.token",
	"io.strategiz.framework.firebase",
	"io.strategiz.framework.secrets",
	"io.strategiz.client.firebase",
	"io.strategiz.client.vault"
})
public class ConsoleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsoleApplication.class, args);
	}

}
