package io.strategiz.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 * Main Application class for Strategiz Core
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "io.strategiz"
},
excludeFilters = {
    @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, pattern = "io.strategiz.client.binanceus.*")
})
@PropertySource("classpath:application.properties")
@EntityScan(basePackages = {"io.strategiz"})
public class Application {

    public static void main(String[] args) {
        try {
            System.out.println("Starting Strategiz Core application...");
            SpringApplication app = new SpringApplication(Application.class);
            app.setAdditionalProfiles("default", "dev");
            app.run(args);
            System.out.println("Strategiz Core application started successfully.");
        } catch (Exception e) {
            System.out.println("Error starting Strategiz Core application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
