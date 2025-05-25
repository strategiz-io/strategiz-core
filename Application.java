package io.strategiz.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    // Application package
    "io.strategiz.application",
    
    // Base packages for both naming conventions
    "io.strategiz",
    "strategiz",
    
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
