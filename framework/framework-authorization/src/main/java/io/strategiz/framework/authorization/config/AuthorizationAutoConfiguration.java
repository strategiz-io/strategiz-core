package io.strategiz.framework.authorization.config;

import dev.paseto.jpaseto.lang.Keys;
import io.strategiz.framework.authorization.filter.PasetoAuthenticationFilter;
import io.strategiz.framework.authorization.resolver.AuthUserArgumentResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

/**
 * Auto-configuration for the authorization framework.
 *
 * <p>This configuration:</p>
 * <ul>
 *   <li>Registers the {@link PasetoAuthenticationFilter} for token extraction</li>
 *   <li>Registers the {@link AuthUserArgumentResolver} for @AuthUser injection</li>
 *   <li>Enables component scanning for aspects and FGA client</li>
 * </ul>
 *
 * <p>To use this framework, add the following to your application.properties:</p>
 * <pre>
 * strategiz.authorization.enabled=true
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthorizationProperties.class)
@ConditionalOnProperty(prefix = "strategiz.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = {
        "io.strategiz.framework.authorization.aspect",
        "io.strategiz.framework.authorization.fga"
})
public class AuthorizationAutoConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationAutoConfiguration.class);

    private final AuthorizationProperties properties;

    @Value("${auth.token.session-key:}")
    private String sessionKeyBase64;

    @Value("${auth.token.identity-key:}")
    private String identityKeyBase64;

    public AuthorizationAutoConfiguration(AuthorizationProperties properties) {
        this.properties = properties;
        log.info("Authorization framework auto-configuration enabled");
    }

    @Bean
    public FilterRegistrationBean<PasetoAuthenticationFilter> pasetoAuthenticationFilter() {
        SecretKey sessionKey = decodeKey(sessionKeyBase64, "session-key");
        SecretKey identityKey = decodeKey(identityKeyBase64, "identity-key");

        PasetoAuthenticationFilter filter = new PasetoAuthenticationFilter(
                sessionKey, identityKey, properties);

        FilterRegistrationBean<PasetoAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); // After CORS
        registration.setName("pasetoAuthenticationFilter");

        log.info("PasetoAuthenticationFilter registered with skip-paths: {}", properties.getSkipPaths());
        return registration;
    }

    @Bean
    public AuthUserArgumentResolver authUserArgumentResolver() {
        return new AuthUserArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserArgumentResolver());
    }

    private SecretKey decodeKey(String base64Key, String keyName) {
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("No {} configured. Token validation will fail. " +
                    "Configure auth.token.{} or use Vault integration.", keyName, keyName);
            // Return a dummy key to avoid NPE - actual validation will fail
            return Keys.secretKey(new byte[32]);
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            return Keys.secretKey(keyBytes);
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 encoding for {}: {}", keyName, e.getMessage());
            return Keys.secretKey(new byte[32]);
        }
    }
}
