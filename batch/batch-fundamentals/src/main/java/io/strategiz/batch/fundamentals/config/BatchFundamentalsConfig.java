package io.strategiz.batch.fundamentals.config;

import io.strategiz.business.fundamentals.config.BusinessFundamentalsConfig;
import io.strategiz.data.fundamentals.config.DataFundamentalsConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "io.strategiz.batch.fundamentals")
@Import({ BusinessFundamentalsConfig.class, DataFundamentalsConfig.class })
public class BatchFundamentalsConfig {

}
