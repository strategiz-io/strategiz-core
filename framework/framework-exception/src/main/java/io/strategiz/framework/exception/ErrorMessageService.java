package io.strategiz.framework.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Service for building error responses from properties files.
 * 
 * Handles message resolution, parameter substitution, and data masking
 * for consistent error responses across the application.
 */
@Service
public class ErrorMessageService {
    
    private static final Logger log = LoggerFactory.getLogger(ErrorMessageService.class);
    
    private final MessageSource messageSource;
    private final String baseDocumentationUrl;
    
    // Patterns for data masking
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\b[A-Za-z0-9]{20,}\\b");
    
    public ErrorMessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
        this.baseDocumentationUrl = "https://docs.strategiz.io/errors/";
    }
    
    /**
     * Build a complete StandardErrorResponse from a StrategizException
     */
    public StandardErrorResponse buildErrorResponse(StrategizException ex) {
        if (!ex.hasErrorDetails()) {
            return buildLegacyErrorResponse(ex);
        }
        
        ErrorDetails errorDetails = ex.getErrorDetails();
        String propertyKey = errorDetails.getPropertyKey();
        Object[] args = ex.getArgs();
        
        try {
            return new StandardErrorResponse(
                getCode(propertyKey),
                getMessage(propertyKey),
                getDeveloperMessage(propertyKey, ex.getModuleName(), args),
                getMoreInfoUrl(propertyKey)
            );
        } catch (Exception e) {
            log.error("Failed to build error response for key: {}", propertyKey, e);
            return buildFallbackErrorResponse(ex);
        }
    }
    
    /**
     * Get the error code from properties
     */
    private String getCode(String propertyKey) {
        try {
            return messageSource.getMessage(propertyKey + ".code", null, Locale.getDefault());
        } catch (NoSuchMessageException e) {
            log.warn("Missing error code for key: {}", propertyKey);
            return propertyKey.toUpperCase().replace("-", "_");
        }
    }
    
    /**
     * Get the user message from properties
     */
    private String getMessage(String propertyKey) {
        try {
            return messageSource.getMessage(propertyKey + ".message", null, Locale.getDefault());
        } catch (NoSuchMessageException e) {
            log.warn("Missing error message for key: {}", propertyKey);
            return "An error occurred. Please try again.";
        }
    }
    
    /**
     * Get the developer message from properties with parameter substitution and masking
     */
    private String getDeveloperMessage(String propertyKey, String moduleName, Object[] args) {
        try {
            String template = messageSource.getMessage(propertyKey + ".developer", null, Locale.getDefault());
            
            // Create enhanced args array with module name as first parameter
            Object[] enhancedArgs = new Object[args.length + 1];
            enhancedArgs[0] = moduleName;
            System.arraycopy(args, 0, enhancedArgs, 1, args.length);
            
            // Format the message with parameters
            String formatted = MessageFormat.format(template, enhancedArgs);
            
            // Apply data masking
            return maskSensitiveData(formatted);
            
        } catch (NoSuchMessageException e) {
            log.warn("Missing developer message for key: {}", propertyKey);
            return "Error occurred in module: " + moduleName;
        } catch (Exception e) {
            log.error("Failed to format developer message for key: {}", propertyKey, e);
            return "Error occurred in module: " + moduleName;
        }
    }
    
    /**
     * Get the documentation URL from properties
     */
    private String getMoreInfoUrl(String propertyKey) {
        try {
            String path = messageSource.getMessage(propertyKey + ".more-info", null, Locale.getDefault());
            return baseDocumentationUrl + path;
        } catch (NoSuchMessageException e) {
            log.warn("Missing more-info path for key: {}", propertyKey);
            return baseDocumentationUrl + "general/error";
        }
    }
    
    /**
     * Mask sensitive data in messages
     */
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
    
    /**
     * Build error response for legacy exceptions (without ErrorDetails)
     */
    private StandardErrorResponse buildLegacyErrorResponse(StrategizException ex) {
        return new StandardErrorResponse(
            ex.getErrorCode(),
            ex.getMessage() != null ? ex.getMessage() : "An error occurred",
            "Error in module: " + ex.getModuleName() + " - " + ex.getMessage(),
            baseDocumentationUrl + "general/error"
        );
    }
    
    /**
     * Build fallback error response when message resolution fails
     */
    private StandardErrorResponse buildFallbackErrorResponse(StrategizException ex) {
        return new StandardErrorResponse(
            ex.getErrorCode(),
            "An error occurred. Please try again.",
            "Error in module: " + ex.getModuleName() + " - " + ex.getErrorCode(),
            baseDocumentationUrl + "general/error"
        );
    }
}