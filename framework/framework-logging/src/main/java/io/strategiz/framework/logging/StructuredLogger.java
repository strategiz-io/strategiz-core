package io.strategiz.framework.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Structured logger utility that provides a fluent API for creating
 * structured log entries with key-value pairs.
 * 
 * Usage examples:
 * 
 * // Simple structured logging
 * StructuredLogger.info()
 *     .operation("user_signup")
 *     .userId("usr_123")
 *     .email("user@example.com")
 *     .log("User signup completed");
 * 
 * // Performance logging
 * StructuredLogger.performance()
 *     .operation("database_query")
 *     .duration(150)
 *     .table("users")
 *     .log("Database query executed");
 * 
 * // Error logging with context
 * StructuredLogger.error()
 *     .operation("payment_processing")
 *     .userId("usr_123")
 *     .amount(99.99)
 *     .errorCode("PAYMENT_FAILED")
 *     .log("Payment processing failed", exception);
 */
public class StructuredLogger {
    
    /**
     * Create a new INFO level structured log entry
     */
    public static LogEntry info() {
        return new LogEntry(LoggerFactory.getLogger(getCallerClass()), LogLevel.INFO);
    }
    
    /**
     * Create a new DEBUG level structured log entry
     */
    public static LogEntry debug() {
        return new LogEntry(LoggerFactory.getLogger(getCallerClass()), LogLevel.DEBUG);
    }
    
    /**
     * Create a new WARN level structured log entry
     */
    public static LogEntry warn() {
        return new LogEntry(LoggerFactory.getLogger(getCallerClass()), LogLevel.WARN);
    }
    
    /**
     * Create a new ERROR level structured log entry
     */
    public static LogEntry error() {
        return new LogEntry(LoggerFactory.getLogger(getCallerClass()), LogLevel.ERROR);
    }
    
    /**
     * Create a performance-focused log entry (INFO level with performance markers)
     */
    public static LogEntry performance() {
        return new LogEntry(LoggerFactory.getLogger(getCallerClass()), LogLevel.INFO)
            .category("performance");
    }
    
    /**
     * Create a business event log entry (INFO level with business markers)
     */
    public static LogEntry business() {
        return new LogEntry(LoggerFactory.getLogger(getCallerClass()), LogLevel.INFO)
            .category("business");
    }
    
    /**
     * Create a security-focused log entry (WARN level with security markers)
     */
    public static LogEntry security() {
        return new LogEntry(LoggerFactory.getLogger(getCallerClass()), LogLevel.WARN)
            .category("security");
    }
    
    /**
     * Create an audit log entry (INFO level with audit markers)
     */
    public static LogEntry audit() {
        return new LogEntry(LoggerFactory.getLogger(getCallerClass()), LogLevel.INFO)
            .category("audit");
    }
    
    /**
     * Get the calling class name for the logger
     */
    private static String getCallerClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Skip getStackTrace(), getCallerClass(), and the StructuredLogger method
        if (stackTrace.length > 3) {
            return stackTrace[3].getClassName();
        }
        return StructuredLogger.class.getName();
    }
    
    /**
     * Fluent log entry builder
     */
    public static class LogEntry {
        private final Logger logger;
        private final LogLevel level;
        private final Map<String, Object> fields = new HashMap<>();
        
        private LogEntry(Logger logger, LogLevel level) {
            this.logger = logger;
            this.level = level;
        }
        
        /**
         * Add an operation identifier
         */
        public LogEntry operation(String operation) {
            return field("operation", operation);
        }
        
        /**
         * Add a user ID
         */
        public LogEntry userId(String userId) {
            return field("userId", userId);
        }
        
        /**
         * Add a session ID
         */
        public LogEntry sessionId(String sessionId) {
            return field("sessionId", sessionId);
        }
        
        /**
         * Add a request ID
         */
        public LogEntry requestId(String requestId) {
            return field("requestId", requestId);
        }
        
        /**
         * Add a component name
         */
        public LogEntry component(String component) {
            return field("component", component);
        }
        
        /**
         * Add a category
         */
        public LogEntry category(String category) {
            return field("category", category);
        }
        
        /**
         * Add a duration in milliseconds
         */
        public LogEntry duration(long durationMs) {
            return field("durationMs", durationMs);
        }
        
        /**
         * Add a status code
         */
        public LogEntry status(int status) {
            return field("status", status);
        }
        
        /**
         * Add an error code
         */
        public LogEntry errorCode(String errorCode) {
            return field("errorCode", errorCode);
        }
        
        /**
         * Add an email (will be masked in production)
         */
        public LogEntry email(String email) {
            return field("email", maskEmail(email));
        }
        
        /**
         * Add an amount
         */
        public LogEntry amount(Object amount) {
            return field("amount", amount);
        }
        
        /**
         * Add a table name
         */
        public LogEntry table(String table) {
            return field("table", table);
        }
        
        /**
         * Add a custom field
         */
        public LogEntry field(String key, Object value) {
            if (key != null && value != null) {
                fields.put(key, value);
            }
            return this;
        }
        
        /**
         * Add multiple fields at once
         */
        public LogEntry fields(Map<String, Object> fields) {
            if (fields != null) {
                this.fields.putAll(fields);
            }
            return this;
        }
        
        /**
         * Log the message
         */
        public void log(String message) {
            log(message, null);
        }
        
        /**
         * Log the message with an exception
         */
        public void log(String message, Throwable throwable) {
            // Add fields to MDC temporarily
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            
            try {
                // Add structured fields to MDC
                for (Map.Entry<String, Object> entry : fields.entrySet()) {
                    MDC.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
                
                // Log based on level
                switch (level) {
                    case DEBUG:
                        if (throwable != null) {
                            logger.debug(message, throwable);
                        } else {
                            logger.debug(message);
                        }
                        break;
                    case INFO:
                        if (throwable != null) {
                            logger.info(message, throwable);
                        } else {
                            logger.info(message);
                        }
                        break;
                    case WARN:
                        if (throwable != null) {
                            logger.warn(message, throwable);
                        } else {
                            logger.warn(message);
                        }
                        break;
                    case ERROR:
                        if (throwable != null) {
                            logger.error(message, throwable);
                        } else {
                            logger.error(message);
                        }
                        break;
                }
            } finally {
                // Restore previous MDC state
                MDC.clear();
                if (previousMdc != null) {
                    MDC.setContextMap(previousMdc);
                }
            }
        }
        
        /**
         * Mask email for privacy
         */
        private String maskEmail(String email) {
            if (email == null || !email.contains("@")) {
                return email;
            }
            
            String[] parts = email.split("@");
            if (parts.length != 2) {
                return email;
            }
            
            String localPart = parts[0];
            String domain = parts[1];
            
            if (localPart.length() <= 2) {
                return "*@" + domain;
            }
            
            return localPart.substring(0, 1) + "*".repeat(localPart.length() - 2) + 
                   localPart.substring(localPart.length() - 1) + "@" + domain;
        }
    }
    
    /**
     * Log levels enum
     */
    private enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}

/**
 * Utility methods for structured logging outside of the fluent API
 */
class LogUtils {

    /**
     * Create a key-value pair for structured logging
     *
     * Usage: log.info("User created {}", kv("userId", userId));
     */
    public static String kv(String key, Object value) {
        return key + "=" + value;
    }

    /**
     * Create multiple key-value pairs
     */
    public static String kvs(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new io.strategiz.framework.exception.StrategizException(
                io.strategiz.framework.logging.exception.LoggingErrorDetails.INVALID_ARGUMENT,
                "LogUtils", "keyValues must have an even number of arguments");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(keyValues[i]).append("=").append(keyValues[i + 1]);
        }
        return sb.toString();
    }
} 