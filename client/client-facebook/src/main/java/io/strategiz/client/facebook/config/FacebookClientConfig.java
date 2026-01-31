package io.strategiz.client.facebook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Facebook client components
 */
@Configuration
public class FacebookClientConfig {

	/**
	 * RestTemplate bean for Facebook API calls
	 * @return RestTemplate configured for Facebook API
	 */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}