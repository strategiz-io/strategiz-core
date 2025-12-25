package io.strategiz.client.execution.config;

import io.strategiz.client.execution.ExecutionServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Strategy Execution gRPC Service Client
 */
@Configuration
public class ExecutionServiceConfig {

	@Value("${strategiz.execution.grpc.host:strategiz-execution-43628135674.us-east1.run.app}")
	private String executionServiceHost;

	@Value("${strategiz.execution.grpc.port:443}")
	private int executionServicePort;

	@Value("${strategiz.execution.grpc.use-tls:true}")
	private boolean useTls;

	@Value("${strategiz.execution.grpc.timeout-seconds:30}")
	private int timeoutSeconds;

	@Bean
	public ExecutionServiceClient executionServiceClient() {
		return new ExecutionServiceClient(executionServiceHost, executionServicePort, useTls, timeoutSeconds);
	}

}
