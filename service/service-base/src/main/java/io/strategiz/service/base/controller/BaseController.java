package io.strategiz.service.base.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;
import io.strategiz.framework.exception.ErrorMessageService;
import io.strategiz.framework.exception.StandardErrorResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Enhanced base controller for all REST controllers in the Strategiz application.
 * Provides common functionality following clean architecture patterns.
 * 
 * Controllers should:
 * - Return clean data objects with ResponseEntity.ok(data)
 * - Throw StrategizException for business errors (handled by GlobalExceptionHandler)
 * - Use response filtering to remove null/empty fields
 * - Follow consistent logging patterns
 * 
 * For authentication-specific functionality, extend BaseAuthenticationController instead.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Strategiz Core API",
        version = "1.0",
        description = "Core backend API for Strategiz platform providing authentication, user management, and business logic services.",
        contact = @Contact(
            name = "Strategiz Development Team",
            email = "dev@strategiz.io"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development Server"),
        @Server(url = "https://api.strategiz.io", description = "Production Server")
    }
)
public abstract class BaseController {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    protected ErrorMessageService errorMessageService;
    
    // Security patterns for input validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
        "^[A-Za-z0-9]+$"
    );
    
    // Request tracking constants
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_START_TIME = "request.start.time";
    
    /**
     * Initialize request tracking - call this at the start of each request
     */
    protected void initializeRequestTracking() {
        String requestId = generateRequestId();
        MDC.put("requestId", requestId);
        MDC.put(REQUEST_START_TIME, String.valueOf(System.currentTimeMillis()));
        
        HttpServletResponse response = getCurrentResponse();
        if (response != null) {
            response.setHeader(REQUEST_ID_HEADER, requestId);
        }
        
        log.debug("Request tracking initialized with ID: {}", requestId);
    }
    
    /**
     * Finalize request tracking - call this at the end of each request
     */
    protected void finalizeRequestTracking() {
        String startTimeStr = MDC.get(REQUEST_START_TIME);
        if (startTimeStr != null) {
            long startTime = Long.parseLong(startTimeStr);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Request completed in {}ms", duration);
        }
        
        // Clean up MDC
        MDC.clear();
    }
    
    /**
     * Generate unique request ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Get current HTTP response
     */
    private HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }
    
    /**
     * Extract client IP address from HTTP request with proxy support
     */
    protected String extractClientIp(HttpServletRequest request) {
        // Check for X-Forwarded-For header (load balancers, proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // Take the first IP in the chain
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check for X-Real-IP header (nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            return xRealIp;
        }
        
        // Additional proxy headers
        String[] headers = {
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP", 
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }
    
    /**
     * Input sanitization for user-provided data
     */
    protected String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Basic sanitization - remove potential XSS/injection attempts
        return input.trim()
            .replaceAll("<script[^>]*>.*?</script>", "")
            .replaceAll("<[^>]+>", "")
            .replaceAll("javascript:", "")
            .replaceAll("on\\w+\\s*=", "");
    }
    
    /**
     * Validate email format
     * @throws StrategizException if email is invalid
     */
    protected void validateEmail(String email, String fieldName) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Invalid email format for field: " + fieldName);
        }
    }
    
    /**
     * Validate phone number format
     * @throws StrategizException if phone number is invalid
     */
    protected void validatePhoneNumber(String phone, String fieldName) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Invalid phone number format for field: " + fieldName);
        }
    }
    
    /**
     * Validate alphanumeric string
     * @throws StrategizException if string contains invalid characters
     */
    protected void validateAlphanumeric(String input, String fieldName) {
        if (input == null || !ALPHANUMERIC_PATTERN.matcher(input).matches()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Field " + fieldName + " must contain only letters and numbers");
        }
    }
    
    /**
     * Validate required parameter
     * @throws StrategizException if parameter is null or empty
     */
    protected void validateRequiredParam(String paramName, Object paramValue) {
        if (paramValue == null) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Required parameter '" + paramName + "' is missing");
        }
        
        if (paramValue instanceof String && ((String) paramValue).trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Required parameter '" + paramName + "' cannot be empty");
        }
    }
    
    /**
     * Validate multiple required parameters
     * @throws StrategizException if any validation fails
     */
    protected void validateRequiredParams(Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            validateRequiredParam(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Business rule validation helper
     * @throws StrategizException if condition is false
     */
    protected void validateBusinessRule(boolean condition, Enum<?> errorCode, String errorMessage) {
        if (!condition) {
            throw new StrategizException(errorCode, errorMessage);
        }
    }
    
    /**
     * Rate limiting check - override in subclasses for actual implementation
     * @throws StrategizException if rate limit is exceeded
     */
    protected void checkRateLimit(String userId, String operation) {
        // Default implementation - override in subclasses with actual rate limiting
        log.debug("Rate limit check for user: {}, operation: {}", userId, operation);
        // Example: throw new StrategizException(ErrorCode.TOO_MANY_REQUESTS, "Too many requests for operation: " + operation);
    }
    
    /**
     * Create clean response with null/empty field filtering
     */
    protected <T> ResponseEntity<T> createCleanResponse(T data) {
        if (data == null) {
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, "Response data cannot be null");
        }
        
        // Filter out null and empty fields
        T cleanedData = filterNullAndEmptyFields(data);
        
        log.debug("Created clean response for type: {}", data.getClass().getSimpleName());
        return ResponseEntity.ok(cleanedData);
    }
    
    /**
     * Create clean response with custom status code
     */
    protected <T> ResponseEntity<T> createCleanResponse(T data, int statusCode) {
        if (data == null) {
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, "Response data cannot be null");
        }
        
        // Filter out null and empty fields
        T cleanedData = filterNullAndEmptyFields(data);
        
        log.debug("Created clean response for type: {} with status: {}", data.getClass().getSimpleName(), statusCode);
        return ResponseEntity.status(statusCode).body(cleanedData);
    }
    
    /**
     * Filter null and empty string fields from response data
     */
    @SuppressWarnings("unchecked")
    private <T> T filterNullAndEmptyFields(T data) {
        try {
            // Convert to JSON, filter, and convert back
            JsonNode jsonNode = objectMapper.valueToTree(data);
            JsonNode filteredNode = filterJsonNode(jsonNode);
            return (T) objectMapper.treeToValue(filteredNode, data.getClass());
        } catch (Exception e) {
            log.warn("Failed to filter null/empty fields, returning original data: {}", e.getMessage());
            return data; // Return original if filtering fails
        }
    }
    
    /**
     * Recursively filter null values and empty strings from JSON
     */
    private JsonNode filterJsonNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            Iterator<String> fieldNames = node.fieldNames();
            
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = node.get(fieldName);
                
                // Skip null values
                if (fieldValue.isNull()) {
                    continue;
                }
                
                // Skip empty strings
                if (fieldValue.isTextual() && fieldValue.asText().trim().isEmpty()) {
                    continue;
                }
                
                // Recursively filter nested objects/arrays
                JsonNode filteredValue = filterJsonNode(fieldValue);
                if (filteredValue != null) {
                    objectNode.set(fieldName, filteredValue);
                }
            }
            
            return objectNode;
        } else if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                JsonNode filteredItem = filterJsonNode(item);
                if (filteredItem != null) {
                    arrayNode.add(filteredItem);
                }
            }
            return arrayNode;
        } else {
            // Return primitive values as-is (already filtered above)
            return node;
        }
    }
    
    /**
     * Log successful request completion
     */
    protected void logRequestSuccess(String operation, String userId, Object result) {
        log.info("SUCCESS - Operation: {}, User: {}, Result: {}", 
            operation, userId, result.getClass().getSimpleName());
    }
    
    /**
     * Log successful request completion with additional context
     */
    protected void logRequestSuccess(String operation, String userId, Object result, String context) {
        log.info("SUCCESS - Operation: {}, User: {}, Result: {}, Context: {}", 
            operation, userId, result.getClass().getSimpleName(), context);
    }
    
    /**
     * Log request with user context
     */
    protected void logRequest(String operation, String userId) {
        log.info("REQUEST - Operation: {}, User: {}", operation, userId);
    }
    
    /**
     * Log request with user context and additional parameters
     */
    protected void logRequest(String operation, String userId, String context) {
        log.info("REQUEST - Operation: {}, User: {}, Context: {}", operation, userId, context);
    }
    
    /**
     * Log request with user context and request parameters
     */
    protected void logRequest(String operation, String userId, Map<String, Object> params) {
        log.info("REQUEST - Operation: {}, User: {}, Params: {}", operation, userId, params);
    }
    
    /**
     * Create standardized error context map
     */
    protected Map<String, Object> createErrorContext(String operation, String userId, String errorType) {
        return Map.of(
            "operation", operation,
            "userId", userId != null ? userId : "unknown",
            "errorType", errorType,
            "timestamp", System.currentTimeMillis()
        );
    }
    
    /**
     * Handle StrategizException (business exceptions) in controllers
     * This is the main exception handler for the hybrid approach
     */
    @ExceptionHandler(StrategizException.class)
    public ResponseEntity<StandardErrorResponse> handleStrategizException(StrategizException ex) {
        String traceId = generateTraceId();
        
        // Log the business exception with module context
        log.warn("Business exception [{}] in module [{}]: {}", 
            traceId, ex.getModuleName(), ex.getErrorCode());
        
        // Build standardized response using ErrorMessageService
        StandardErrorResponse errorResponse = errorMessageService.buildErrorResponse(ex);
        
        // Add trace ID and module info to response headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Trace-Id", traceId);
        headers.add("X-Error-Module", ex.getModuleName());
        
        // Add trace ID to MDC for any subsequent logging
        MDC.put("traceId", traceId);
        
        return ResponseEntity.status(ex.getHttpStatus()).headers(headers).body(errorResponse);
    }
    
    /**
     * Build error response - can be overridden by subclasses for customization
     */
    protected ResponseEntity<StandardErrorResponse> buildErrorResponse(StrategizException ex) {
        StandardErrorResponse response = errorMessageService.buildErrorResponse(ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }
    
    /**
     * Generate a unique trace ID for error tracking
     */
    private String generateTraceId() {
        return generateRequestId(); // Reuse the existing method
    }
    
    /**
     * Fallback exception handler for unexpected exceptions
     * GlobalExceptionHandler should handle most cases, but this provides a safety net
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardErrorResponse> handleUnexpectedException(Exception ex) {
        String traceId = generateTraceId();
        
        log.error("Unexpected error in controller [{}]: {}", traceId, ex.getMessage(), ex);
        
        StandardErrorResponse errorResponse = new StandardErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support.",
            "Unexpected exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
            "https://docs.strategiz.io/errors/general/internal-error"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Trace-Id", traceId);
        headers.add("X-Error-Module", "unknown");
        
        MDC.put("traceId", traceId);
        
        return ResponseEntity.internalServerError().headers(headers).body(errorResponse);
    }
}
