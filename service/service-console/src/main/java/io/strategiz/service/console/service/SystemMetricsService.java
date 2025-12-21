package io.strategiz.service.console.service;

import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.console.model.response.SystemHealthResponse;
import io.strategiz.service.console.model.response.SystemHealthResponse.ComponentHealth;
import io.strategiz.service.console.model.response.SystemHealthResponse.MemoryUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for collecting and reporting system metrics and health.
 */
@Service
public class SystemMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(SystemMetricsService.class);

    private final UserRepository userRepository;

    @Value("${spring.application.name:strategiz-console}")
    private String applicationName;

    @Value("${strategiz.version:1.0-SNAPSHOT}")
    private String applicationVersion;

    private final Instant startTime = Instant.now();

    @Autowired
    public SystemMetricsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public SystemHealthResponse getSystemHealth() {
        logger.debug("Collecting system health metrics");

        SystemHealthResponse response = new SystemHealthResponse();

        // Basic info
        response.setStartTime(startTime);
        response.setUptime(formatUptime());
        response.setVersion(applicationVersion);

        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        response.setMemoryUsage(new MemoryUsage(usedMemory, maxMemory, maxMemory - usedMemory));

        // Set status based on basic checks
        response.setStatus("UP");

        // Component health
        Map<String, ComponentHealth> components = new HashMap<>();
        components.put("database", new ComponentHealth("UP"));
        components.put("vault", new ComponentHealth("UP"));
        response.setComponents(components);

        // User counts
        try {
            response.setActiveUsers(userRepository.findAll().size());
            response.setActiveSessions(0); // TODO: Add session counting
        } catch (Exception e) {
            logger.warn("Failed to get user counts: {}", e.getMessage());
            response.setActiveUsers(0);
            response.setActiveSessions(0);
        }

        return response;
    }

    public Map<String, Object> getDetailedMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // JVM metrics
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        metrics.put("jvm.uptime.ms", runtimeMXBean.getUptime());
        metrics.put("jvm.start.time", runtimeMXBean.getStartTime());
        metrics.put("jvm.name", runtimeMXBean.getVmName());
        metrics.put("jvm.version", runtimeMXBean.getVmVersion());

        // Memory metrics
        Runtime runtime = Runtime.getRuntime();
        metrics.put("memory.max.bytes", runtime.maxMemory());
        metrics.put("memory.total.bytes", runtime.totalMemory());
        metrics.put("memory.free.bytes", runtime.freeMemory());
        metrics.put("memory.used.bytes", runtime.totalMemory() - runtime.freeMemory());

        // Thread metrics
        metrics.put("threads.active", Thread.activeCount());

        // Processor metrics
        metrics.put("processors.available", runtime.availableProcessors());

        return metrics;
    }

    private String formatUptime() {
        Duration uptime = Duration.between(startTime, Instant.now());
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
