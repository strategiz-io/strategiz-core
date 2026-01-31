package io.strategiz.service.console.config;

import io.strategiz.business.aichat.config.BusinessAiChatConfig;
import io.strategiz.business.cryptotoken.config.BusinessCryptoTokenConfig;
import io.strategiz.business.historicalinsights.config.BusinessHistoricalInsightsConfig;
import io.strategiz.business.infrastructurecosts.config.BusinessInfrastructureCostsConfig;
import io.strategiz.business.preferences.config.BusinessPreferencesConfig;
import io.strategiz.business.risk.config.BusinessRiskConfig;
import io.strategiz.business.tokenauth.config.BusinessTokenAuthConfig;
import io.strategiz.data.featureflags.config.DataFeatureFlagsConfig;
import io.strategiz.data.session.config.DataSessionConfig;
import io.strategiz.data.testing.config.DataTestingConfig;
import io.strategiz.data.user.config.DataUserConfig;
import io.strategiz.data.watchlist.config.DataWatchlistConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = "io.strategiz.service.console")
@Import({ BusinessTokenAuthConfig.class, BusinessInfrastructureCostsConfig.class, BusinessAiChatConfig.class,
		BusinessCryptoTokenConfig.class, BusinessPreferencesConfig.class, BusinessRiskConfig.class,
		BusinessHistoricalInsightsConfig.class, DataUserConfig.class, DataSessionConfig.class,
		DataFeatureFlagsConfig.class, DataTestingConfig.class, DataWatchlistConfig.class })
public class ServiceConsoleConfig {

}
