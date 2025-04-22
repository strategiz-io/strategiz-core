package io.strategiz.trading.agent.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Gemini AI Trading Agent
 * Sets up configuration and necessary beans for the trading agent
 */
@Configuration
public class GeminiTradingAgentConfig {

    @Value("${gemini.model.name:gemini-pro}")
    private String modelName;
    
    @Value("${gemini.model.temperature:0.2}")
    private float temperature;
    
    @Value("${gemini.model.max-output-tokens:8192}")
    private int maxOutputTokens;
    
    @Value("${trading.agent.default-timeframe:1d}")
    private String defaultTimeframe;
    
    @Value("${trading.agent.default-risk-profile:moderate}")
    private String defaultRiskProfile;
    
    /**
     * Create a RestTemplate specific for Gemini API calls
     * Renamed from restTemplate to avoid conflicts with WebConfig bean
     * 
     * @return RestTemplate configured for Gemini API
     */
    @Bean
    public RestTemplate geminiRestTemplate() {
        return new RestTemplate();
    }
}
