package io.strategiz.application;

import io.strategiz.api.auth.config.ApiAuthConfig;
import io.strategiz.api.monitoring.config.ApiMonitoringConfig;
import io.strategiz.api.portfolio.config.ApiPortfolioConfig;
import io.strategiz.api.strategy.config.ApiStrategyConfig;
import io.strategiz.api.exchange.config.ApiExchangeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Main Application class for Strategiz Core
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "io.strategiz.application",
    "io.strategiz"
})
@Import({
    // API module configurations
    ApiAuthConfig.class,
    ApiMonitoringConfig.class,
    ApiPortfolioConfig.class,
    ApiStrategyConfig.class,
    ApiExchangeConfig.class
})
public class Application {

    public static void main(String[] args) {
        try {
            System.out.println("Starting Strategiz Core application...");
            SpringApplication.run(Application.class, args);
            System.out.println("Strategiz Core application started successfully.");
        } catch (Exception e) {
            System.out.println("Error starting Strategiz Core application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
