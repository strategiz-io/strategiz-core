package io.strategiz.service.auth.bdd;

import io.strategiz.service.auth.service.passkey.PasskeyAuthenticationService;
import io.strategiz.service.auth.service.passkey.PasskeyChallengeService;
import io.strategiz.service.auth.service.totp.TotpService;
import io.strategiz.service.auth.service.sms.SmsVerificationService;
import io.strategiz.service.auth.service.email.EmailOtpService;
import io.strategiz.service.auth.service.email.MagicLinkService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for Authentication BDD tests.
 *
 * Provides mocked authentication services to enable BDD tests to run
 * without requiring real SMS/email delivery, WebAuthn hardware, or external API calls.
 *
 * Excludes Firebase/Firestore and data source auto-configuration to avoid
 * requiring GCP credentials and database connections in tests.
 *
 * PasetoTokenValidator is mocked via @MockBean in CucumberSpringConfiguration.
 *
 * NOTE: These are UNIT tests with mocked services - they verify authentication
 * business logic and flows. For INTEGRATION tests with real services,
 * use a separate test suite with proper credentials and infrastructure.
 */
@SpringBootApplication(exclude = {
    com.google.cloud.spring.autoconfigure.firestore.GcpFirestoreAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.firestore.FirestoreTransactionManagerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
public class AuthBDDTestConfiguration {

    /**
     * Mock Passkey Authentication Service.
     *
     * Simulates WebAuthn/FIDO2 registration and authentication without requiring
     * actual hardware security keys or platform authenticators.
     */
    @Bean
    @Primary
    public PasskeyAuthenticationService mockPasskeyAuthenticationService() {
        return new PasskeyAuthenticationService(null, null) {
            @Override
            protected String getModuleName() {
                return "test-passkey";
            }

            // Mock methods would be implemented here
            // For now, step definitions handle the logic
        };
    }

    /**
     * Mock Passkey Challenge Service.
     *
     * Simulates WebAuthn challenge generation and validation.
     */
    @Bean
    @Primary
    public PasskeyChallengeService mockPasskeyChallengeService() {
        return new PasskeyChallengeService(null) {
            @Override
            protected String getModuleName() {
                return "test-passkey-challenge";
            }

            // Mock methods would be implemented here
        };
    }

    /**
     * Mock TOTP Service.
     *
     * Simulates TOTP (Google Authenticator) setup, verification, and backup code management
     * without requiring actual TOTP secret generation or time-based validation.
     */
    @Bean
    @Primary
    public TotpService mockTotpService() {
        return new TotpService(null, null) {
            @Override
            protected String getModuleName() {
                return "test-totp";
            }

            // Mock methods would be implemented here
        };
    }

    /**
     * Mock SMS Verification Service.
     *
     * Simulates SMS code delivery and verification without requiring
     * actual SMS gateway integration or phone number validation.
     */
    @Bean
    @Primary
    public SmsVerificationService mockSmsVerificationService() {
        return new SmsVerificationService(null, null) {
            @Override
            protected String getModuleName() {
                return "test-sms";
            }

            // Mock methods would be implemented here
        };
    }

    /**
     * Mock Email OTP Service.
     *
     * Simulates email OTP code delivery and verification without requiring
     * actual email service integration.
     */
    @Bean
    @Primary
    public EmailOtpService mockEmailOtpService() {
        return new EmailOtpService(null, null) {
            @Override
            protected String getModuleName() {
                return "test-email-otp";
            }

            // Mock methods would be implemented here
        };
    }

    /**
     * Mock Magic Link Service.
     *
     * Simulates magic link generation and validation without requiring
     * actual email delivery or token storage.
     */
    @Bean
    @Primary
    public MagicLinkService mockMagicLinkService() {
        return new MagicLinkService(null, null) {
            @Override
            protected String getModuleName() {
                return "test-magic-link";
            }

            // Mock methods would be implemented here
        };
    }
}
