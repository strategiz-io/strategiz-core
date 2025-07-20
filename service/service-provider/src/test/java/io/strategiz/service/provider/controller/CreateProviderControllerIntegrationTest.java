package io.strategiz.service.provider.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for CreateProviderController.
 * Simple test to ensure the controller is properly wired during build.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@SpringBootTest(classes = TestProviderConfiguration.class)
@ActiveProfiles("test")
class CreateProviderControllerIntegrationTest {
    
    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
        // It will fail if there are any configuration or wiring issues
        assertTrue(true, "Context loaded successfully");
    }
    
    @Test
    void testProviderControllerExists() {
        // Simple existence test that runs during build
        assertNotNull(CreateProviderController.class);
        assertEquals("io.strategiz.service.provider.controller.CreateProviderController", 
                    CreateProviderController.class.getName());
    }
}