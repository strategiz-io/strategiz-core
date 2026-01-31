package io.strategiz.service.auth.service.signup;

import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.base.constants.SubscriptionTierConstants;
import io.strategiz.data.base.transaction.FirestoreTransactionTemplate;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.signup.SignupTokenService.SignupTokenClaims;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for atomically creating user accounts during signup Step 2.
 *
 * This service is called when a user completes auth method registration
 * (passkey/TOTP/SMS) during signup. It creates the user account, email
 * reservation, EMAIL_OTP auth method, and the chosen auth method all within a
 * single Firestore transaction.
 */
@Service
public class AccountCreationService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthenticationMethodRepository authenticationMethodRepository;

	@Autowired
	private FirestoreTransactionTemplate transactionTemplate;

	@Autowired
	private EmailReservationService emailReservationService;

	@Autowired(required = false)
	private SubscriptionService subscriptionService;

	@Value("${email.signup.admin.emails:}")
	private String adminEmails;

	/**
	 * Create a user account atomically with email reservation and EMAIL_OTP auth
	 * method. This is called from auth method registration controllers during the
	 * signup flow.
	 * @param claims The validated signup token claims
	 * @return The created user entity
	 */
	public UserEntity createAccount(SignupTokenClaims claims) {
		String email = claims.email();
		String userId = claims.userId();
		String name = claims.name();

		log.info("Creating account for email: {} with userId: {}", email, userId);

		try {
			UserEntity createdUser = transactionTemplate.execute(transaction -> {
				// Create CONFIRMED email reservation (fails if email already taken)
				emailReservationService.createConfirmedReservation(email, userId, "email_otp");

				UserProfileEntity profile = new UserProfileEntity(name, email, null, true,
						SubscriptionTierConstants.DEFAULT, true);

				if (isAdminEmail(email)) {
					profile.setRole("ADMIN");
					log.info("Setting ADMIN role for admin email: {}", email);
				}

				UserEntity user = new UserEntity();
				user.setUserId(userId);
				user.setProfile(profile);

				UserEntity created = userRepository.createUser(user);

				// Create EMAIL_OTP authentication method
				AuthenticationMethodEntity authMethod = new AuthenticationMethodEntity(
						AuthenticationMethodType.EMAIL_OTP, "Email: " + email);
				authMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS, email);
				authMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED, true);
				authMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.VERIFICATION_TIME,
						Instant.now().toString());
				authMethod.setIsActive(true);

				authenticationMethodRepository.saveForUser(created.getUserId(), authMethod);
				log.info("Created EMAIL_OTP authentication method for user: {}", created.getUserId());

				return created;
			});

			// Initialize trial subscription (outside transaction)
			if (subscriptionService != null) {
				try {
					subscriptionService.initializeTrial(createdUser.getUserId());
					log.info("Initialized 30-day trial for user: {}", createdUser.getUserId());
				}
				catch (Exception e) {
					log.warn("Failed to initialize trial subscription: {}", e.getMessage());
				}
			}

			return createdUser;

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error creating account for email {}: {}", email, e.getMessage(), e);
			throw new StrategizException(AuthErrors.SIGNUP_FAILED, "Failed to create account");
		}
	}

	private boolean isAdminEmail(String email) {
		if (adminEmails == null || adminEmails.isBlank()) {
			return false;
		}
		String[] emails = adminEmails.split(",");
		for (String adminEmail : emails) {
			if (adminEmail.trim().equalsIgnoreCase(email)) {
				return true;
			}
		}
		return false;
	}

}
