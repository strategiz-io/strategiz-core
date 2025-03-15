package io.strategiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
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

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        System.out.println("Configuring CORS settings...");
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
                System.out.println("CORS settings configured successfully.");
            }
        };
    }
}
