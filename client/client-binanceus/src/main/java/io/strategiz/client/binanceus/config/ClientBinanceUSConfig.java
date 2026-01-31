package io.strategiz.client.binanceus.config;

import io.strategiz.client.binanceus.BinanceUSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Binance US API client Following the Synapse client framework pattern
 * for imperative programming
 */
@Configuration
public class ClientBinanceUSConfig {

	private static final Logger log = LoggerFactory.getLogger(ClientBinanceUSConfig.class);

	/**
	 * Creates the BinanceUSClient instance using the base client framework This leverages
	 * the BaseHttpClient and ExchangeApiClient from client-base With Spring property
	 * injection for credentials and URL
	 * @return Configured BinanceUSClient
	 */
	@Bean
	public BinanceUSClient binanceUSClient() {
		log.info("Initializing BinanceUS API Client using Spring property injection");
		// The default constructor will use Spring property injection
		// @PostConstruct init() method will set up all required values
		return new BinanceUSClient();
	}

}
