package io.strategiz.application;

import io.strategiz.api.auth.config.AuthConfig;
import io.strategiz.api.monitoring.config.MonitoringConfig;
import io.strategiz.api.portfolio.config.PortfolioConfig;
import io.strategiz.api.strategy.config.StrategyConfig;
import strategiz.api.exchange.config.ExchangeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan(basePackages = {"io.strategiz.application"})
@Import({
    // API module configurations
    AuthConfig.class,
    MonitoringConfig.class,
    PortfolioConfig.class,
    StrategyConfig.class,
    ExchangeConfig.class
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
