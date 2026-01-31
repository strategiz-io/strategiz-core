package io.strategiz.batch.livestrategies.config;

import io.strategiz.data.strategy.config.DataStrategyConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "io.strategiz.batch.livestrategies")
@Import({ DataStrategyConfig.class })
public class BatchLiveStrategiesConfig {

}
