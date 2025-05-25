package io.strategiz.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "io.strategiz",
    // API modules
    "io.strategiz.api.auth",
    "io.strategiz.api.exchange",
    "io.strategiz.api.portfolio",
    "io.strategiz.api.strategy",
    "io.strategiz.api.monitoring",
    // Client modules
    "io.strategiz.client.kraken",
    // Data modules
    "io.strategiz.data.auth",
    "io.strategiz.data.exchange",
    "io.strategiz.data.portfolio",
    "io.strategiz.data.strategy",
    "io.strategiz.data.document.storage",
    // Service modules
    "io.strategiz.service.auth",
    "io.strategiz.service.exchange",
    "io.strategiz.service.portfolio",
    "io.strategiz.service.strategy",
    // Legacy packages (to maintain backward compatibility)
    "io.strategiz.coinbase",
    "io.strategiz.coinbase.controller",
    "io.strategiz.coinbase.service",
    "io.strategiz.binanceus",
    "io.strategiz.kraken"
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
