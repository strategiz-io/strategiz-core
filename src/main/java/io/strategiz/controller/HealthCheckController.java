package io.strategiz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple health check controller to verify the application is running
 */
@RestController
public class HealthCheckController {

    /**
     * Basic health check endpoint
     * @return Status information
     */
    @GetMapping("/api/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Strategiz Core API is running");
        response.put("timestamp", System.currentTimeMillis());
        
        System.out.println("Health check endpoint called");
        return response;
    }
}
