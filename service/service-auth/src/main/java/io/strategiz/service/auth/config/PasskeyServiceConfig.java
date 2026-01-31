package io.strategiz.service.auth.config;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.converter.PasskeyCredentialConverter;
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
	public PasskeyCredentialConverter passkeyCredentialConverter() {
		return new PasskeyCredentialConverter();
	}

	@Bean
	public PasskeyRegistrationService passkeyRegistrationService(PasskeyChallengeService challengeService,
			AuthenticationMethodRepository authMethodRepository, SessionAuthBusiness sessionAuthBusiness,
			UserRepository userRepository, FeatureFlagService featureFlagService) {
		return new PasskeyRegistrationService(challengeService, authMethodRepository, sessionAuthBusiness,
				userRepository, featureFlagService);
	}

	@Bean
	public PasskeyAuthenticationService passkeyAuthenticationService(PasskeyChallengeService challengeService,
			AuthenticationMethodRepository authMethodRepository, SessionAuthBusiness sessionAuthBusiness,
			UserRepository userRepository) {
		return new PasskeyAuthenticationService(challengeService, authMethodRepository, sessionAuthBusiness,
				userRepository);
	}

	@Bean
	public PasskeyManagementService passkeyManagementService(PasskeyCredentialRepository credentialRepository,
			PasskeyCredentialConverter credentialConverter) {
		return new PasskeyManagementService(credentialRepository, credentialConverter);
	}

}
