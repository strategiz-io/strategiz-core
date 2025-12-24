package io.strategiz.execution.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import io.strategiz.execution.service.StrategyExecutionServiceImpl;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Server for Strategy Execution Service
 *
 * Listens on port 50051 and handles strategy execution requests from main API.
 * Includes health checks and reflection for debugging.
 */
@Component
public class GrpcServer {

    private static final Logger logger = LoggerFactory.getLogger(GrpcServer.class);

    private final Server server;
    private final StrategyExecutionServiceImpl executionService;
    private final HealthStatusManager healthStatusManager;

    @Value("${grpc.server.port:50051}")
    private int port;

    public GrpcServer(StrategyExecutionServiceImpl executionService) {
        this.executionService = executionService;
        this.healthStatusManager = new HealthStatusManager();
        this.server = createServer();
    }

    private Server createServer() {
        return ServerBuilder.forPort(port)
            // Main execution service
            .addService(executionService)

            // Health check service
            .addService(healthStatusManager.getHealthService())

            // Reflection service (for debugging with grpcurl)
            .addService(ProtoReflectionService.newInstance())

            // Configure max message size (50MB for large market data)
            .maxInboundMessageSize(50 * 1024 * 1024)

            // Configure thread pool
            .executor(java.util.concurrent.Executors.newFixedThreadPool(20))

            .build();
    }

    /**
     * Start the gRPC server
     */
    public void start() throws IOException {
        server.start();

        // Mark service as healthy
        healthStatusManager.setStatus("", io.grpc.health.v1.HealthCheckResponse.ServingStatus.SERVING);

        logger.info("gRPC Server started on port {}", port);
        logger.info("Services registered:");
        logger.info("  - strategiz.execution.v1.StrategyExecutionService");
        logger.info("  - grpc.health.v1.Health");
        logger.info("  - grpc.reflection.v1alpha.ServerReflection");

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server (JVM shutdown hook)");
            try {
                GrpcServer.this.stop();
            } catch (InterruptedException e) {
                logger.error("Error during shutdown", e);
            }
        }));
    }

    /**
     * Stop the server
     */
    @PreDestroy
    public void stop() throws InterruptedException {
        if (server != null) {
            logger.info("Stopping gRPC server...");

            // Mark service as not serving
            healthStatusManager.setStatus("", io.grpc.health.v1.HealthCheckResponse.ServingStatus.NOT_SERVING);

            // Graceful shutdown with 30s timeout
            server.shutdown();

            if (!server.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Server did not terminate gracefully, forcing shutdown");
                server.shutdownNow();
                server.awaitTermination(5, TimeUnit.SECONDS);
            }

            logger.info("gRPC server stopped");
        }
    }

    /**
     * Block until the server shuts down
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Get server port
     */
    public int getPort() {
        return server != null ? server.getPort() : -1;
    }
}
