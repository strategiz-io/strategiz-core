package io.strategiz.execution;

import io.strategiz.execution.server.GrpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Strategy Execution Service - Isolated gRPC service for executing trading strategies
 *
 * This service is completely isolated from the main API for security and performance:
 * - Runs on separate Cloud Run instance
 * - Independent scaling (0-100 instances)
 * - Network boundary isolation
 * - Dedicated CPU/memory resources
 *
 * Supports:
 * - Python strategies (via subprocess with sandboxing)
 * - Java strategies (native execution with ta4j)
 * - Technical indicators (ta4j library)
 * - Backtesting and performance metrics
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "io.strategiz.execution",
    "io.strategiz.framework",
    "io.strategiz.business.strategy.execution"
})
public class ExecutionServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionServiceApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Strategy Execution Service...");

        // Start Spring Boot application
        ConfigurableApplicationContext context = SpringApplication.run(ExecutionServiceApplication.class, args);

        // Start gRPC server
        GrpcServer grpcServer = context.getBean(GrpcServer.class);

        try {
            grpcServer.start();
            logger.info("Strategy Execution Service started successfully");

            // Block main thread to keep server running
            grpcServer.blockUntilShutdown();

        } catch (Exception e) {
            logger.error("Failed to start gRPC server", e);
            System.exit(1);
        }
    }
}
