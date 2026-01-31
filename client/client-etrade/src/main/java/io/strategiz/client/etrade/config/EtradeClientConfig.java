package io.strategiz.client.etrade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for E*TRADE API client. Provides RestTemplate bean for HTTP
 * communications.
 */
@Configuration
public class EtradeClientConfig {

	@Bean(name = "etradeRestTemplate")
	public RestTemplate etradeRestTemplate() {
		return new RestTemplate();
	}

}
