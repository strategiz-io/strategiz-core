package io.strategiz.data.featureflags.service;

import io.strategiz.data.featureflags.entity.FeatureFlagEntity;
import io.strategiz.data.featureflags.repository.FeatureFlagRepository;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing feature flags.
 * Provides caching for fast flag lookups and methods for CRUD operations.
 */
@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    // Cache for fast lookups (refreshed periodically or on update)
    private final Map<String, Boolean> flagCache = new ConcurrentHashMap<>();

    private final FeatureFlagRepository repository;

    // Well-known feature flag IDs

    // Provider flags
    public static final String FLAG_PLAID_ENABLED = "plaid_enabled";
    public static final String FLAG_ROBINHOOD_ENABLED = "robinhood_enabled";
    public static final String FLAG_TRADING_ENABLED = "trading_enabled";

    // Platform flags
    public static final String FLAG_PRE_LAUNCH_MODE = "pre_launch_mode_enabled";

    // Auth flags
    public static final String FLAG_AUTH_EMAIL_OTP_SIGNUP = "auth_email_otp_signup_enabled";
    public static final String FLAG_AUTH_PASSKEY_SIGNUP = "auth_passkey_signup_enabled";
    public static final String FLAG_AUTH_TOTP_SIGNUP = "auth_totp_signup_enabled";
    public static final String FLAG_AUTH_SMS_OTP_SIGNUP = "auth_sms_otp_signup_enabled";
    public static final String FLAG_AUTH_OAUTH_SIGNUP = "auth_oauth_signup_enabled";

    // AI - Holistic level (entire features)
    public static final String FLAG_AI_LEARN_ENABLED = "ai_learn_enabled";
    public static final String FLAG_AI_LABS_ENABLED = "ai_labs_enabled";
    public static final String FLAG_HISTORICAL_INSIGHTS = "ai_historical_insights_enabled";

    // AI - Provider level (all models from a provider)
    public static final String FLAG_AI_PROVIDER_GEMINI = "ai_provider_gemini_enabled";
    public static final String FLAG_AI_PROVIDER_CLAUDE = "ai_provider_claude_enabled";
    public static final String FLAG_AI_PROVIDER_OPENAI = "ai_provider_openai_enabled";
    public static final String FLAG_AI_PROVIDER_META = "ai_provider_meta_enabled";
    public static final String FLAG_AI_PROVIDER_MISTRAL = "ai_provider_mistral_enabled";
    public static final String FLAG_AI_PROVIDER_COHERE = "ai_provider_cohere_enabled";

    // AI - Model level (individual models)
    public static final String FLAG_AI_MODEL_GEMINI_2_5_FLASH = "ai_model_gemini_2_5_flash";
    public static final String FLAG_AI_MODEL_GEMINI_2_5_PRO = "ai_model_gemini_2_5_pro";
    public static final String FLAG_AI_MODEL_GEMINI_1_5_FLASH = "ai_model_gemini_1_5_flash";
    public static final String FLAG_AI_MODEL_GEMINI_1_5_PRO = "ai_model_gemini_1_5_pro";
    public static final String FLAG_AI_MODEL_GPT_4O_MINI = "ai_model_gpt_4o_mini";
    public static final String FLAG_AI_MODEL_GPT_4O = "ai_model_gpt_4o";
    public static final String FLAG_AI_MODEL_O1_MINI = "ai_model_o1_mini";
    public static final String FLAG_AI_MODEL_O1 = "ai_model_o1";
    public static final String FLAG_AI_MODEL_CLAUDE_HAIKU_4_5 = "ai_model_claude_haiku_4_5";
    public static final String FLAG_AI_MODEL_CLAUDE_SONNET_4_5 = "ai_model_claude_sonnet_4_5";
    public static final String FLAG_AI_MODEL_CLAUDE_OPUS_4_5 = "ai_model_claude_opus_4_5";

    @Autowired
    public FeatureFlagService(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void initializeDefaultFlags() {
        log.info("Initializing default feature flags...");

        // Provider flags
        createDefaultFlag(FLAG_PLAID_ENABLED, "Plaid Integration",
            "Enable Plaid Link for connecting brokerage accounts", false, "providers");

        createDefaultFlag(FLAG_ROBINHOOD_ENABLED, "Robinhood Integration",
            "Enable direct Robinhood credential-based integration", true, "providers");

        createDefaultFlag(FLAG_TRADING_ENABLED, "Live Trading",
            "Enable live trading (paper trading always available)", false, "trading");

        // Platform flags
        createDefaultFlag(FLAG_PRE_LAUNCH_MODE, "Pre-Launch Mode",
            "Show pre-launch waitlist page instead of main landing page", false, "platform");

        // Auth flags
        createDefaultFlag(FLAG_AUTH_EMAIL_OTP_SIGNUP, "Email OTP Signup",
            "Enable email OTP verification during sign up", true, "auth");

        createDefaultFlag(FLAG_AUTH_PASSKEY_SIGNUP, "Passkey Signup",
            "Enable passkey (WebAuthn) authentication during sign up", true, "auth");

        createDefaultFlag(FLAG_AUTH_TOTP_SIGNUP, "TOTP Signup",
            "Enable TOTP (Google Authenticator) authentication during sign up", true, "auth");

        createDefaultFlag(FLAG_AUTH_SMS_OTP_SIGNUP, "SMS OTP Signup",
            "Enable SMS OTP verification during sign up", false, "auth");

        createDefaultFlag(FLAG_AUTH_OAUTH_SIGNUP, "OAuth Signup",
            "Enable OAuth provider authentication during sign up (Google, Facebook, etc.)", true, "auth");

        // AI - Holistic level
        createDefaultFlag(FLAG_AI_LEARN_ENABLED, "AI Learn Chat",
            "Enable Learn AI Chat assistant for trading education", true, "ai");

        createDefaultFlag(FLAG_AI_LABS_ENABLED, "AI Labs Strategy Generator",
            "Enable Labs AI-powered strategy generation", true, "ai");

        createDefaultFlag(FLAG_HISTORICAL_INSIGHTS, "AI Historical Market Insights",
            "Enable Historical Market Insights with 7 years of data analysis (Autonomous AI)", true, "ai");

        // AI - Provider level
        createDefaultFlag(FLAG_AI_PROVIDER_GEMINI, "Gemini Provider",
            "Enable all Google Gemini models", true, "ai");

        createDefaultFlag(FLAG_AI_PROVIDER_CLAUDE, "Claude Provider",
            "Enable all Anthropic Claude models via Vertex AI", true, "ai");

        createDefaultFlag(FLAG_AI_PROVIDER_OPENAI, "OpenAI Provider",
            "Enable all OpenAI models (GPT-4, o1, etc.)", true, "ai");

        createDefaultFlag(FLAG_AI_PROVIDER_META, "Meta Llama Provider",
            "Enable all Meta Llama models via Vertex AI", true, "ai");

        createDefaultFlag(FLAG_AI_PROVIDER_MISTRAL, "Mistral AI Provider",
            "Enable all Mistral AI models via Vertex AI", true, "ai");

        createDefaultFlag(FLAG_AI_PROVIDER_COHERE, "Cohere Provider",
            "Enable all Cohere models via Vertex AI", true, "ai");

        // AI - Model level (Gemini)
        createDefaultFlag(FLAG_AI_MODEL_GEMINI_2_5_FLASH, "Gemini 2.5 Flash",
            "Fast and capable, balanced performance", true, "ai");

        createDefaultFlag(FLAG_AI_MODEL_GEMINI_2_5_PRO, "Gemini 2.5 Pro",
            "High-capability model for complex reasoning", true, "ai");

        createDefaultFlag(FLAG_AI_MODEL_GEMINI_1_5_FLASH, "Gemini 1.5 Flash",
            "Fast and efficient", true, "ai");

        createDefaultFlag(FLAG_AI_MODEL_GEMINI_1_5_PRO, "Gemini 1.5 Pro",
            "Stable production model", true, "ai");

        // AI - Model level (OpenAI)
        createDefaultFlag(FLAG_AI_MODEL_GPT_4O_MINI, "GPT-4o Mini",
            "Fast & affordable OpenAI model", true, "ai");

        createDefaultFlag(FLAG_AI_MODEL_GPT_4O, "GPT-4o",
            "Powerful multimodal flagship model", true, "ai");

        createDefaultFlag(FLAG_AI_MODEL_O1_MINI, "o1 Mini",
            "Fast reasoning model", true, "ai");

        createDefaultFlag(FLAG_AI_MODEL_O1, "o1",
            "Best reasoning model (expensive)", true, "ai");

        // AI - Model level (Claude)
        createDefaultFlag(FLAG_AI_MODEL_CLAUDE_HAIKU_4_5, "Claude Haiku 4.5",
            "Fast & affordable Claude model", true, "ai");

        createDefaultFlag(FLAG_AI_MODEL_CLAUDE_SONNET_4_5, "Claude Sonnet 4.5",
            "Balanced performance Claude model", true, "ai");

        createDefaultFlag(FLAG_AI_MODEL_CLAUDE_OPUS_4_5, "Claude Opus 4.5",
            "Best for coding & agents (expensive)", true, "ai");

        // Refresh cache after initialization
        refreshCache();
    }

    private void createDefaultFlag(String flagId, String name, String description,
                                    boolean defaultEnabled, String category) {
        if (repository.findById(flagId).isEmpty()) {
            FeatureFlagEntity flag = new FeatureFlagEntity(flagId, name, description, defaultEnabled, category);
            repository.save(flag);
            log.info("Created default feature flag: {} = {}", flagId, defaultEnabled);
        }
    }

    /**
     * Refresh the flag cache from the database.
     */
    public void refreshCache() {
        log.info("Refreshing feature flag cache...");
        flagCache.clear();
        List<FeatureFlagEntity> flags = repository.findAll();
        for (FeatureFlagEntity flag : flags) {
            flagCache.put(flag.getFlagId(), flag.isEnabled());
        }
        log.info("Feature flag cache refreshed with {} flags", flagCache.size());
    }

    /**
     * Check if a feature is enabled (uses cache for performance).
     */
    public boolean isEnabled(String flagId) {
        // Check cache first
        Boolean cached = flagCache.get(flagId);
        if (cached != null) {
            return cached;
        }

        // Fall back to database
        boolean enabled = repository.isEnabled(flagId);
        flagCache.put(flagId, enabled);
        return enabled;
    }

    /**
     * Check if Plaid integration is enabled.
     */
    public boolean isPlaidEnabled() {
        return isEnabled(FLAG_PLAID_ENABLED);
    }

    /**
     * Check if Robinhood integration is enabled.
     */
    public boolean isRobinhoodEnabled() {
        return isEnabled(FLAG_ROBINHOOD_ENABLED);
    }

    /**
     * Check if Learn AI Chat is enabled.
     */
    public boolean isLearnAIEnabled() {
        return isEnabled(FLAG_AI_LEARN_ENABLED);
    }

    /**
     * Check if Labs AI Strategy Generator is enabled.
     */
    public boolean isLabsAIEnabled() {
        return isEnabled(FLAG_AI_LABS_ENABLED);
    }

    /**
     * Check if Historical Market Insights is enabled (Autonomous AI mode).
     * Analyzes 7 years of historical data to generate optimized strategies.
     */
    public boolean isHistoricalInsightsEnabled() {
        return isEnabled(FLAG_HISTORICAL_INSIGHTS);
    }

    /**
     * Check if Pre-Launch Mode is enabled.
     * When enabled, show pre-launch waitlist page instead of main landing.
     */
    public boolean isPreLaunchMode() {
        return isEnabled(FLAG_PRE_LAUNCH_MODE);
    }

    /**
     * Check if Email OTP signup is enabled.
     */
    public boolean isEmailOtpSignupEnabled() {
        return isEnabled(FLAG_AUTH_EMAIL_OTP_SIGNUP);
    }

    /**
     * Check if Passkey signup is enabled.
     */
    public boolean isPasskeySignupEnabled() {
        return isEnabled(FLAG_AUTH_PASSKEY_SIGNUP);
    }

    /**
     * Check if TOTP signup is enabled.
     */
    public boolean isTotpSignupEnabled() {
        return isEnabled(FLAG_AUTH_TOTP_SIGNUP);
    }

    /**
     * Check if SMS OTP signup is enabled.
     */
    public boolean isSmsOtpSignupEnabled() {
        return isEnabled(FLAG_AUTH_SMS_OTP_SIGNUP);
    }

    /**
     * Check if OAuth signup is enabled.
     */
    public boolean isOAuthSignupEnabled() {
        return isEnabled(FLAG_AUTH_OAUTH_SIGNUP);
    }

    /**
     * Check if a specific AI model is enabled (checks both provider and model flags).
     * Hierarchy: Feature > Provider > Model (all must be enabled)
     */
    public boolean isAIModelEnabled(String modelId) {
        // Determine provider from model ID
        String providerFlag;
        String modelFlag;

        if (modelId.startsWith("gemini")) {
            providerFlag = FLAG_AI_PROVIDER_GEMINI;
            modelFlag = getModelFlagForId(modelId);
        } else if (modelId.startsWith("gpt") || modelId.startsWith("o1")) {
            providerFlag = FLAG_AI_PROVIDER_OPENAI;
            modelFlag = getModelFlagForId(modelId);
        } else if (modelId.startsWith("claude")) {
            providerFlag = FLAG_AI_PROVIDER_CLAUDE;
            modelFlag = getModelFlagForId(modelId);
        } else if (modelId.startsWith("llama")) {
            providerFlag = FLAG_AI_PROVIDER_META;
            modelFlag = null; // No model-specific flags for Llama yet
        } else if (modelId.startsWith("mistral")) {
            providerFlag = FLAG_AI_PROVIDER_MISTRAL;
            modelFlag = null; // No model-specific flags for Mistral yet
        } else if (modelId.startsWith("command")) {
            providerFlag = FLAG_AI_PROVIDER_COHERE;
            modelFlag = null; // No model-specific flags for Cohere yet
        } else {
            log.warn("Unknown model provider for: {}", modelId);
            return false;
        }

        // Check provider flag first (if provider disabled, all models disabled)
        if (!isEnabled(providerFlag)) {
            log.debug("Provider {} disabled for model {}", providerFlag, modelId);
            return false;
        }

        // Check model-specific flag
        if (modelFlag != null && !isEnabled(modelFlag)) {
            log.debug("Model flag {} disabled", modelFlag);
            return false;
        }

        return true;
    }

    /**
     * Map model ID to feature flag constant.
     */
    private String getModelFlagForId(String modelId) {
        return switch (modelId) {
            case "gemini-2.5-flash" -> FLAG_AI_MODEL_GEMINI_2_5_FLASH;
            case "gemini-2.5-pro" -> FLAG_AI_MODEL_GEMINI_2_5_PRO;
            case "gemini-1.5-flash" -> FLAG_AI_MODEL_GEMINI_1_5_FLASH;
            case "gemini-1.5-pro" -> FLAG_AI_MODEL_GEMINI_1_5_PRO;
            case "gpt-4o-mini" -> FLAG_AI_MODEL_GPT_4O_MINI;
            case "gpt-4o" -> FLAG_AI_MODEL_GPT_4O;
            case "o1-mini" -> FLAG_AI_MODEL_O1_MINI;
            case "o1" -> FLAG_AI_MODEL_O1;
            case "claude-haiku-4-5" -> FLAG_AI_MODEL_CLAUDE_HAIKU_4_5;
            case "claude-sonnet-4-5" -> FLAG_AI_MODEL_CLAUDE_SONNET_4_5;
            case "claude-opus-4-5" -> FLAG_AI_MODEL_CLAUDE_OPUS_4_5;
            default -> null;
        };
    }

    /**
     * Get all feature flags.
     */
    public List<FeatureFlagEntity> getAllFlags() {
        return repository.findAll();
    }

    /**
     * Get feature flags by category.
     */
    public List<FeatureFlagEntity> getFlagsByCategory(String category) {
        return repository.findByCategory(category);
    }

    /**
     * Get a specific feature flag.
     */
    public Optional<FeatureFlagEntity> getFlag(String flagId) {
        return repository.findById(flagId);
    }

    /**
     * Enable a feature flag.
     */
    public FeatureFlagEntity enableFlag(String flagId) {
        return setFlagEnabled(flagId, true);
    }

    /**
     * Disable a feature flag.
     */
    public FeatureFlagEntity disableFlag(String flagId) {
        return setFlagEnabled(flagId, false);
    }

    /**
     * Set a feature flag's enabled state.
     */
    public FeatureFlagEntity setFlagEnabled(String flagId, boolean enabled) {
        Optional<FeatureFlagEntity> optFlag = repository.findById(flagId);
        if (optFlag.isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND,
                "FeatureFlagEntity", "Feature flag not found: " + flagId);
        }

        FeatureFlagEntity flag = optFlag.get();
        flag.setEnabled(enabled);
        FeatureFlagEntity saved = repository.save(flag);

        // Update cache
        flagCache.put(flagId, enabled);
        log.info("Feature flag {} set to {}", flagId, enabled);

        return saved;
    }

    /**
     * Create a new feature flag.
     */
    public FeatureFlagEntity createFlag(FeatureFlagEntity flag) {
        if (repository.findById(flag.getFlagId()).isPresent()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.DUPLICATE_ENTITY,
                "FeatureFlagEntity", "Feature flag already exists: " + flag.getFlagId());
        }

        FeatureFlagEntity saved = repository.save(flag);
        flagCache.put(flag.getFlagId(), flag.isEnabled());
        return saved;
    }

    /**
     * Update a feature flag.
     */
    public FeatureFlagEntity updateFlag(FeatureFlagEntity flag) {
        if (repository.findById(flag.getFlagId()).isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND,
                "FeatureFlagEntity", "Feature flag not found: " + flag.getFlagId());
        }

        FeatureFlagEntity saved = repository.save(flag);
        flagCache.put(flag.getFlagId(), flag.isEnabled());
        return saved;
    }

    /**
     * Delete a feature flag.
     */
    public void deleteFlag(String flagId) {
        repository.delete(flagId);
        flagCache.remove(flagId);
        log.info("Feature flag deleted: {}", flagId);
    }
}
