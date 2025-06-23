package io.strategiz.service.auth.config;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.data.auth.repository.passkey.challenge.PasskeyChallengeRepository;
import io.strategiz.service.auth.service.passkey.PasskeyAuthenticationService;
import io.strategiz.service.auth.service.passkey.PasskeyChallengeService;
import io.strategiz.service.auth.service.passkey.PasskeyManagementService;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for passkey authentication services
 */
@Configuration
public class PasskeyServiceConfig {

    @Bean
    public PasskeyChallengeService passkeyChallengeBeanService(
            PasskeyChallengeRepository challengeRepository) {
        return new PasskeyChallengeService(challengeRepository);
    }
    
    @Bean
    public PasskeyRegistrationService passkeyRegistrationService(
            PasskeyChallengeService challengeService,
            PasskeyCredentialRepository credentialRepository,
            SessionAuthBusiness sessionAuthBusiness) {
        return new PasskeyRegistrationService(
            challengeService, credentialRepository, sessionAuthBusiness);
    }
    
    @Bean
    public PasskeyAuthenticationService passkeyAuthenticationService(
            PasskeyChallengeService challengeService,
            PasskeyCredentialRepository credentialRepository,
            SessionAuthBusiness sessionAuthBusiness) {
        return new PasskeyAuthenticationService(
            challengeService, credentialRepository, sessionAuthBusiness);
    }
    
    @Bean
    public PasskeyManagementService passkeyManagementService(
            PasskeyCredentialRepository credentialRepository) {
        return new PasskeyManagementService(credentialRepository);
    }
}
