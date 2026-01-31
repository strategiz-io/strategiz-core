package io.strategiz.service.marketing.config;

import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.alphavantage.AlphaVantageClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for service-marketing module Provides mock beans and test-specific
 * configurations for market data clients
 */
@TestConfiguration
@Profile("test")
public class MarketingTestConfiguration {

	@Bean
	@Primary
	public CoinbaseClient mockCoinbaseClient() {
		return Mockito.mock(CoinbaseClient.class);
	}

	@Bean
	@Primary
	public CoinGeckoClient mockCoinGeckoClient() {
		return Mockito.mock(CoinGeckoClient.class);
	}

	@Bean
	@Primary
	public AlphaVantageClient mockAlphaVantageClient() {
		return Mockito.mock(AlphaVantageClient.class);
	}

}