package io.strategiz.service.auth.service.signup;

import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.data.base.transaction.FirestoreTransactionTemplate;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.signup.OAuthSignupRequest;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
import io.strategiz.service.auth.service.fraud.FraudDetectionService;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling OAuth signup processes
 * Specifically designed for signup flows where profile data comes from OAuth providers
 */
@Service
public class SignupService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final SignupResponseBuilder responseBuilder;
    private final FirestoreTransactionTemplate transactionTemplate;
    private final AuthenticationMethodRepository authenticationMethodRepository;
    private final FraudDetectionService fraudDetectionService;
    private final SubscriptionService subscriptionService;
    private final FeatureFlagService featureFlagService;

    @Autowired
    public SignupService(
        UserRepository userRepository,
        UserFactory userFactory,
        SignupResponseBuilder responseBuilder,
        FirestoreTransactionTemplate transactionTemplate,
        AuthenticationMethodRepository authenticationMethodRepository,
        FeatureFlagService featureFlagService,
        @Autowired(required = false) FraudDetectionService fraudDetectionService,
        @Autowired(required = false) SubscriptionService subscriptionService
    ) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.responseBuilder = responseBuilder;
        this.transactionTemplate = transactionTemplate;
        this.authenticationMethodRepository = authenticationMethodRepository;
        this.featureFlagService = featureFlagService;
        this.fraudDetectionService = fraudDetectionService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Process OAuth signup with profile data from external provider.
     * Uses Firestore transaction to ensure atomic user creation with email uniqueness check.
     *
     * @param request OAuth signup request containing OAuth profile data
     * @param deviceId Device ID for token generation
     * @param ipAddress IP address for token generation
     * @return OAuthSignupResponse with user details and authentication tokens
     */
    public OAuthSignupResponse processSignup(OAuthSignupRequest request, String deviceId, String ipAddress) {
        String authMethod = request.getAuthMethod().toLowerCase();
        log.info("=====> SIGNUP SERVICE: Processing OAuth signup for email: {} with auth method: {}", request.getEmail(), authMethod);

        // Check if OAuth signup is enabled
        if (!featureFlagService.isOAuthSignupEnabled()) {
            log.warn("OAuth signup is disabled - rejecting signup for email: {}", request.getEmail());
            throw new StrategizException(AuthErrors.AUTH_METHOD_DISABLED,
                "OAuth signup is currently disabled");
        }

        try {
            // Verify reCAPTCHA token for fraud detection (before any database operations)
            if (fraudDetectionService != null) {
                fraudDetectionService.verifySignup(request.getRecaptchaToken(), request.getEmail());
                log.info("=====> SIGNUP SERVICE: reCAPTCHA verification passed for email: {}", request.getEmail());
            }

            // Create the user entity with profile information using the factory
            UserEntity user = userFactory.createUser(request);
            String createdBy = request.getEmail();

            // Execute user creation within a Firestore transaction
            // This ensures atomic check-and-create to prevent duplicate users
            // CRITICAL: Security subcollection is created INSIDE transaction for atomicity
            UserEntity createdUser = transactionTemplate.execute(transaction -> {
                // Check if user already exists (within transaction for consistency)
                if (userRepository.getUserByEmail(request.getEmail()).isPresent()) {
                    throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "User with email already exists");
                }

                // Create user - this will use the transaction from ThreadLocal
                UserEntity created = userRepository.createUser(user);

                // Create security subcollection INSIDE transaction for atomicity
                // SubcollectionRepository is now transaction-aware and will use the active transaction
                log.info("=====> SECURITY_INIT: Creating OAuth authentication method inside transaction for user: {}", created.getUserId());
                AuthenticationMethodEntity authMethodEntity = buildOAuthAuthenticationMethod(request.getAuthMethod(), createdBy);
                authenticationMethodRepository.saveForUser(created.getUserId(), authMethodEntity);
                log.info("=====> SECURITY_INIT: Successfully saved OAuth authentication method for user: {}", created.getUserId());

                return created;
            });

            log.info("=====> SIGNUP SERVICE: OAuth user created successfully in transaction with security subcollection: {}", createdUser.getUserId());

            // Initialize 30-day trial subscription for new user
            // Done outside transaction - subscription can be created separately
            if (subscriptionService != null) {
                try {
                    subscriptionService.initializeTrial(createdUser.getUserId());
                    log.info("=====> SIGNUP SERVICE: Initialized 30-day trial for user: {}", createdUser.getUserId());
                } catch (Exception e) {
                    // Non-fatal - subscription will be created lazily if this fails
                    log.warn("Failed to initialize trial subscription for user {}: {}", createdUser.getUserId(), e.getMessage());
                }
            }

            // Watchlist initialization removed - Dashboard will create watchlist on-demand
            // This makes signup instant and prevents failures from external API issues

            // Build success response with tokens (outside transaction - tokens don't need atomicity)
            List<String> authMethods = List.of(authMethod);
            return responseBuilder.buildSuccessResponse(
                createdUser,
                "OAuth signup completed successfully",
                authMethods,
                deviceId,
                ipAddress
            );

        } catch (StrategizException e) {
            log.warn("OAuth signup failed for {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during OAuth signup for {}: {}", request.getEmail(), e.getMessage(), e);
            throw new StrategizException(AuthErrors.SIGNUP_FAILED, "OAuth signup failed due to internal error");
        }
    }

    /**
     * Build OAuth authentication method entity (pure function - no I/O).
     * This method is called INSIDE a transaction to ensure atomicity.
     *
     * @param authMethod The OAuth provider (e.g., "google", "facebook")
     * @param createdBy Email of the user who created this
     * @return AuthenticationMethodEntity ready to be saved
     */
    private AuthenticationMethodEntity buildOAuthAuthenticationMethod(String authMethod, String createdBy) {
        // Determine the authentication method type based on the auth method
        AuthenticationMethodType authType;
        String displayName;

        switch (authMethod.toLowerCase()) {
            case "google":
                authType = AuthenticationMethodType.OAUTH_GOOGLE;
                displayName = "Google Account";
                break;
            case "facebook":
                authType = AuthenticationMethodType.OAUTH_FACEBOOK;
                displayName = "Facebook Account";
                break;
            case "microsoft":
                authType = AuthenticationMethodType.OAUTH_MICROSOFT;
                displayName = "Microsoft Account";
                break;
            case "github":
                authType = AuthenticationMethodType.OAUTH_GITHUB;
                displayName = "GitHub Account";
                break;
            default:
                log.warn("Unknown OAuth provider: {}, defaulting to OAUTH_GOOGLE", authMethod);
                authType = AuthenticationMethodType.OAUTH_GOOGLE;
                displayName = "OAuth Account";
        }

        // Create authentication method entity
        // NOTE: Do NOT manually set audit fields (createdBy, modifiedBy, isActive)
        // The repository layer's _initAudit() method will initialize them automatically
        AuthenticationMethodEntity authMethodEntity = new AuthenticationMethodEntity();
        authMethodEntity.setAuthenticationMethod(authType);
        authMethodEntity.setName(displayName);
        authMethodEntity.setLastUsedAt(Instant.now());

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", authMethod.toLowerCase());
        metadata.put("registeredAt", Instant.now().toString());
        authMethodEntity.setMetadata(metadata);

        return authMethodEntity;
    }

}