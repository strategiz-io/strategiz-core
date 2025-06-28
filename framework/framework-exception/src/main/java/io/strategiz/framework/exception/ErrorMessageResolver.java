package io.strategiz.framework.exception;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Resolves error messages from message resource bundles for internationalization.
 * Falls back to the error definition's default message if the message key is not found.
 */
@Component
public class ErrorMessageResolver {
    private final MessageSource messageSource;
    
    public ErrorMessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
    
    /**
     * Resolve a message for an error definition, with internationalization support
     * 
     * @param errorDefinition the error definition
     * @param locale the locale to use
     * @param args message arguments
     * @return the resolved message
     */
    public String resolveMessage(ErrorDefinition errorDefinition, Locale locale, Object... args) {
        String messageKey = "error." + errorDefinition.getDomain() + "." + errorDefinition.name().toLowerCase();
        
        try {
            return messageSource.getMessage(messageKey, args, locale);
        } catch (NoSuchMessageException e) {
            // Fall back to the default message if not found in message bundles
            return errorDefinition.formatMessage(args);
        }
    }
    
    /**
     * Resolve a message key directly
     * 
     * @param messageKey the message key
     * @param locale the locale to use
     * @param args message arguments
     * @return the resolved message
     */
    public String resolveMessage(String messageKey, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(messageKey, args, locale);
        } catch (NoSuchMessageException e) {
            // If message key not found, return a placeholder
            return "???" + messageKey + "???";
        }
    }
}
