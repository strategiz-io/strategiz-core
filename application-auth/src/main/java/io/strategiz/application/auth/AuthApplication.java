package io.strategiz.application.auth;

import io.strategiz.application.auth.config.VaultOAuthInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auth Application - Dedicated authentication service for Strategiz platform.
 *
 * Serves all authentication endpoints at /v1/auth/*:
 * - /v1/auth/oauth/google/* - Google OAuth sign-in/sign-up
 * - /v1/auth/oauth/facebook/* - Facebook OAuth sign-in/sign-up
 * - /v1/auth/totp/* - TOTP (Authenticator) registration and authentication
 * - /v1/auth/sms-otp/* - SMS OTP registration and authentication
 * - /v1/auth/passkeys/* - WebAuthn/Passkey registration, authentication, management
 * - /v1/auth/push/* - Push notification authentication
 * - /v1/auth/session/* - Session management
 * - /v1/auth/signout - Sign out
 * - /v1/auth/recovery/* - Account recovery
 * - /v1/auth/security/* - Security settings
 * - /v1/auth/token/* - Cross-app SSO token management
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
@EnableCaching
@ComponentScan(basePackages = {
    // Auth Application
    "io.strategiz.application.auth",

    // Auth Service (all auth controllers)
    "io.strategiz.service.auth",

    // Business Layer
    "io.strategiz.business.tokenauth",
    "io.strategiz.business.provider.kraken",
    "io.strategiz.business.provider.binanceus",
    "io.strategiz.business.provider.coinbase",

    // Data Layer
    "io.strategiz.data.auth",
    "io.strategiz.data.session",
    "io.strategiz.data.user",
    "io.strategiz.data.watchlist",
    "io.strategiz.data.framework",
    "io.strategiz.data.base",

    // Framework
    "io.strategiz.framework.authorization",
    "io.strategiz.framework.token",
    "io.strategiz.framework.firebase",
    "io.strategiz.framework.secrets",
    "io.strategiz.framework.exception",
    "io.strategiz.framework.apidocs",

    // Clients
    "io.strategiz.client.firebase",
    "io.strategiz.client.vault",
    "io.strategiz.client.google",
    "io.strategiz.client.facebook",
    "io.strategiz.client.firebasesms",
    "io.strategiz.client.webpush",
    "io.strategiz.client.coinbase",
    "io.strategiz.client.coingecko",
    "io.strategiz.client.yahoofinance",
    "io.strategiz.client.recaptcha",

    // Service Framework Base (CorsFilter, SecurityConfig, BaseController)
    "io.strategiz.service.base"
})
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AuthApplication.class);
        // Add Vault OAuth initializer for loading OAuth credentials early
        app.addInitializers(new VaultOAuthInitializer());
        app.run(args);
    }

}
