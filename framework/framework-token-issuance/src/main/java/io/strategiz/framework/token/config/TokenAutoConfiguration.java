package io.strategiz.framework.token.config;

import io.strategiz.framework.token.issuer.PasetoTokenIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the token issuance framework.
 * Provides automatic bean registration for PasetoTokenIssuer.
 */
@Configuration
@ComponentScan(basePackages = "io.strategiz.framework.token")
public class TokenAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TokenAutoConfiguration.class);

    /**
     * Creates the PasetoTokenIssuer bean if not already defined.
     */
    @Bean
    @ConditionalOnMissingBean
    public PasetoTokenIssuer pasetoTokenIssuer() {
        log.info("Creating PasetoTokenIssuer bean");
        return new PasetoTokenIssuer();
    }
}
