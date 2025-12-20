package io.strategiz.service.monitoring.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry configuration for distributed tracing and observability.
 *
 * <p>This configuration enables:
 * <ul>
 *   <li>Distributed tracing with trace context propagation</li>
 *   <li>Automatic instrumentation via Micrometer Observation API</li>
 *   <li>Integration with OTLP collector for exporting traces</li>
 *   <li>Correlation between traces, metrics, and logs</li>
 * </ul>
 *
 * <p>Traces are exported to the OpenTelemetry Collector which forwards them to:
 * <ul>
 *   <li>Tempo for trace storage and visualization</li>
 *   <li>Mimir for metrics (via span metrics)</li>
 *   <li>Loki for correlated logs</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class OpenTelemetryConfig {

	private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

	@Value("${otel.service.name:strategiz-core}")
	private String serviceName;

	@Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
	private String otlpEndpoint;

	/**
	 * Creates an ObservedAspect bean to enable @Observed annotation support.
	 * This allows methods to be automatically traced by annotating them with @Observed.
	 * @param observationRegistry the observation registry
	 * @return the observed aspect
	 */
	@Bean
	@ConditionalOnClass(name = "io.micrometer.observation.aop.ObservedAspect")
	public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
		logger.info("Initializing OpenTelemetry tracing for service: {} -> {}", serviceName, otlpEndpoint);
		return new ObservedAspect(observationRegistry);
	}

}
