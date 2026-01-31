package io.strategiz.batch.marketdata.config;

import io.strategiz.business.marketdata.config.BusinessMarketDataConfig;
import io.strategiz.data.marketdata.config.DataMarketDataConfig;
import io.strategiz.data.symbol.config.DataSymbolConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "io.strategiz.batch.marketdata")
@Import({ BusinessMarketDataConfig.class, DataMarketDataConfig.class, DataSymbolConfig.class })
public class BatchMarketDataConfig {

}
