package io.strategiz.service.console.config;

import io.strategiz.service.console.websocket.TestStreamingHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time test execution log streaming Enables streaming of
 * test output to connected clients via WebSocket
 */
@Configuration
@EnableWebSocket
public class TestStreamingWebSocketConfig implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(testStreamingHandler(), "/ws/test-stream/{runId}")
			.setAllowedOrigins("http://localhost:3001", "https://console.strategiz.io",
					"https://strategiz-console.web.app");
	}

	@Bean
	public TestStreamingHandler testStreamingHandler() {
		return new TestStreamingHandler();
	}

}
