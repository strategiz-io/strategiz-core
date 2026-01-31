package io.strategiz.framework.exception;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;

/**
 * Service for building error responses from properties files.
 *
 * <p>
 * Handles message resolution, parameter substitution, and data masking for consistent
 * error. responses across the application.
 */
@Service
public class ErrorMessageService implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(ErrorMessageService.class);

	private final MessageSource messageSource;

	private final String baseDocumentationUrl;

	// Patterns for data masking
	private static final Pattern EMAIL_PATTERN = Pattern
		.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

	private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\b[A-Za-z0-9]{20,}\\b");

	/**
	 * Creates an ErrorMessageService with the provided message source.
	 * @param messageSource the message source for localized error messages.
	 */
	@Autowired
	public ErrorMessageService(MessageSource messageSource) {
		this.messageSource = messageSource;
		this.baseDocumentationUrl = "https://docs.strategiz.io/errors/";
	}

	@Override
	public void afterPropertiesSet() {
		log.info("ErrorMessageService initialized with MessageSource: {}", messageSource.getClass().getName());
		// Verify that error properties are resolvable
		try {
			String testCode = messageSource.getMessage("query-execution-failed.code", null, Locale.getDefault());
			log.info("Message source verification passed: query-execution-failed.code -> {}", testCode);
		}
		catch (NoSuchMessageException e) {
			log.warn("Message source verification FAILED: cannot resolve 'query-execution-failed.code'. "
					+ "Injected MessageSource type: {}. "
					+ "Ensure GlobalMessageSourceConfig is loaded with data-base-errors basename.",
					messageSource.getClass().getName());
			// Attempt to add basenames if the injected source is a ResourceBundleMessageSource
			if (messageSource instanceof ResourceBundleMessageSource rbms) {
				log.info("Adding error message basenames to ResourceBundleMessageSource as fallback");
				rbms.addBasenames("messages/data-base-errors", "messages/service-auth-errors",
						"messages/service-base-errors");
			}
		}
	}

	/** Build a complete StandardErrorResponse from a StrategizException. */
	public StandardErrorResponse buildErrorResponse(StrategizException ex) {
		if (!ex.hasErrorDetails()) {
			return buildLegacyErrorResponse(ex);
		}

		ErrorDetails errorDetails = ex.getErrorDetails();
		String propertyKey = errorDetails.getPropertyKey();
		Object[] args = ex.getArgs();

		log.info("Building error response for property key: {}", propertyKey);

		try {
			StandardErrorResponse response = new StandardErrorResponse(getCode(propertyKey), getMessage(propertyKey),
					getDeveloperMessage(propertyKey, ex.getModuleName(), args), getMoreInfoUrl(propertyKey));
			log.info("Built response - code: {}, message: {}", response.getCode(), response.getMessage());
			return response;
		}
		catch (Exception e) {
			log.error("Failed to build error response for key: {}", propertyKey, e);
			return buildFallbackErrorResponse(ex);
		}
	}

	/** Get the error code from properties. */
	private String getCode(String propertyKey) {
		try {
			String messageKey = propertyKey + ".code";
			log.debug("Looking up message key: {}", messageKey);
			String code = messageSource.getMessage(messageKey, null, Locale.getDefault());
			log.debug("Found code: {}", code);
			return code;
		}
		catch (NoSuchMessageException e) {
			log.warn("Missing error code for key: {}", propertyKey);
			// Return a human-readable code derived from the property key
			return propertyKey.toUpperCase().replace('-', '_');
		}
	}

	/** Get the user message from properties. */
	private String getMessage(String propertyKey) {
		try {
			String messageKey = propertyKey + ".message";
			log.debug("Looking up message key: {}", messageKey);
			String message = messageSource.getMessage(messageKey, null, Locale.getDefault());
			log.debug("Found message: {}", message);
			return message;
		}
		catch (NoSuchMessageException e) {
			log.warn("Missing error message for key: {}", propertyKey);
			return "An error occurred. Please try again.";
		}
	}

	/**
	 * Get the developer message from properties with parameter substitution and masking.
	 */
	private String getDeveloperMessage(String propertyKey, String moduleName, Object[] args) {
		try {
			String messageKey = propertyKey + ".developer";
			log.debug("Looking up developer message key: {}", messageKey);
			String template = messageSource.getMessage(messageKey, null, Locale.getDefault());
			log.debug("Found developer message template: {}", template);

			// Create enhanced args array with module name as first parameter
			Object[] enhancedArgs = new Object[args.length + 1];
			enhancedArgs[0] = moduleName;
			System.arraycopy(args, 0, enhancedArgs, 1, args.length);

			// Format the message with parameters
			String formatted = MessageFormat.format(template, enhancedArgs);

			// Apply data masking
			return maskSensitiveData(formatted);

		}
		catch (NoSuchMessageException e) {
			log.warn("Missing developer message for key: {}", propertyKey);
			return "Error occurred in module: " + moduleName;
		}
		catch (Exception e) {
			log.error("Failed to format developer message for key: {}", propertyKey, e);
			return "Error occurred in module: " + moduleName;
		}
	}

	/** Get the documentation URL from properties. */
	private String getMoreInfoUrl(String propertyKey) {
		try {
			String messageKey = propertyKey + ".more-info";
			log.debug("Looking up more-info key: {}", messageKey);
			String path = messageSource.getMessage(messageKey, null, Locale.getDefault());
			log.debug("Found more-info path: {}", path);
			return baseDocumentationUrl + path;
		}
		catch (NoSuchMessageException e) {
			log.warn("Missing more-info path for key: {}", propertyKey);
			return baseDocumentationUrl + propertyKey;
		}
	}

	/** Mask sensitive data in messages. */
	private String maskSensitiveData(String message) {
		if (message == null) {
			return null;
		}

		String masked = message;

		// Mask email addresses
		masked = EMAIL_PATTERN.matcher(masked).replaceAll("[MASKED_EMAIL]");

		// Mask IP addresses (keep first two octets)
		masked = IP_PATTERN.matcher(masked).replaceAll(matchResult -> {
			String ip = matchResult.group();
			String[] parts = ip.split("\\.");
			return parts[0] + "." + parts[1] + ".x.x";
		});

		// Mask tokens (keep first 3 and last 3 characters)
		masked = TOKEN_PATTERN.matcher(masked).replaceAll(matchResult -> {
			String token = matchResult.group();
			if (token.length() <= 6) {
				return "[MASKED_TOKEN]";
			}
			return token.substring(0, 3) + "***" + token.substring(token.length() - 3);
		});

		return masked;
	}

	/** Build error response for legacy exceptions (without ErrorDetails). */
	private StandardErrorResponse buildLegacyErrorResponse(StrategizException ex) {
		return new StandardErrorResponse(ex.getErrorCode(),
				ex.getMessage() != null ? ex.getMessage() : "An error occurred",
				"Error in module: " + ex.getModuleName() + " - " + ex.getMessage(),
				baseDocumentationUrl + "general/error");
	}

	/** Build fallback error response when message resolution fails. */
	private StandardErrorResponse buildFallbackErrorResponse(StrategizException ex) {
		return new StandardErrorResponse(ex.getErrorCode(), "An error occurred. Please try again.",
				"Error in module: " + ex.getModuleName() + " - " + ex.getErrorCode(),
				baseDocumentationUrl + "general/error");
	}

}
