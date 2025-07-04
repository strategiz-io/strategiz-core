package io.strategiz.framework.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.UUID;

/**
 * Enhanced servlet filter that sets up request context information for logging.
 * This filter runs at the highest precedence to ensure context is set
 * as early as possible in the request lifecycle.
 * 
 * Features:
 * - Request correlation ID generation
 * - Request timing and performance logging
 * - User context extraction from headers/sessions
 * - Structured logging context setup
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    
    // Headers to check for correlation IDs
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    
    // Performance logging threshold (ms)
    private static final long SLOW_REQUEST_THRESHOLD = 1000;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest httpRequest) || 
            !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            setupLoggingContext(httpRequest);
            logRequestStart(httpRequest);
            
            chain.doFilter(request, response);
            
        } finally {
            stopWatch.stop();
            logRequestEnd(httpRequest, httpResponse, stopWatch.getTotalTimeMillis());
            cleanupLoggingContext();
        }
    }
    
    /**
     * Set up logging context with request information
     */
    private void setupLoggingContext(HttpServletRequest request) {
        // Generate or extract correlation ID
        String correlationId = extractOrGenerateCorrelationId(request);
        String requestId = extractOrGenerateRequestId(request);
        
        // Extract request information
        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String userId = extractUserId(request);
        
        // Set up MDC for logback
        MDC.put(LoggingContext.CORRELATION_ID, correlationId);
        MDC.put(LoggingContext.REQUEST_ID, requestId);
        MDC.put(LoggingContext.REQUEST_PATH, path);
        MDC.put(LoggingContext.REQUEST_METHOD, method);
        MDC.put(LoggingContext.REQUEST_IP, ip);
        
        if (userId != null) {
            MDC.put(LoggingContext.USER_ID, userId);
        }
        
        // Set up our logging context
        LoggingContext.put(LoggingContext.CORRELATION_ID, correlationId);
        LoggingContext.put(LoggingContext.REQUEST_ID, requestId);
        LoggingContext.put(LoggingContext.REQUEST_PATH, path);
        LoggingContext.put(LoggingContext.REQUEST_METHOD, method);
        LoggingContext.put(LoggingContext.REQUEST_IP, ip);
        
        if (userId != null) {
            LoggingContext.put(LoggingContext.USER_ID, userId);
        }
    }
    
    /**
     * Extract or generate correlation ID
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return correlationId;
    }
    
    /**
     * Extract or generate request ID
     */
    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = "req-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return requestId;
    }
    
    /**
     * Extract user ID from various sources
     */
    private String extractUserId(HttpServletRequest request) {
        // Try header first
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }
        
        // Try session attribute
        if (request.getSession(false) != null) {
            Object sessionUserId = request.getSession(false).getAttribute("userId");
            if (sessionUserId != null) {
                return sessionUserId.toString();
            }
        }
        
        // Could add JWT token parsing here if needed
        return null;
    }
    
    /**
     * Get the real client IP address, handling proxies
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Log request start
     */
    private void logRequestStart(HttpServletRequest request) {
        log.debug("Request started: {} {}", 
            request.getMethod(), 
            request.getRequestURI()
        );
    }
    
    /**
     * Log request completion with performance metrics
     */
    private void logRequestEnd(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        int statusCode = response.getStatus();
        String method = request.getMethod();
        String path = request.getRequestURI();
        
        if (durationMs > SLOW_REQUEST_THRESHOLD) {
            log.warn("Slow request completed: {} {} - {} - {}ms", 
                method, path, statusCode, durationMs);
        } else if (statusCode >= 400) {
            log.warn("Request completed with error: {} {} - {} - {}ms", 
                method, path, statusCode, durationMs);
        } else {
            log.debug("Request completed: {} {} - {} - {}ms", 
                method, path, statusCode, durationMs);
        }
        
        // Log performance metrics
        StructuredLogger.performance()
            .operation("http_request")
            .duration(durationMs)
            .status(statusCode)
            .log("HTTP request completed");
    }
    
    /**
     * Clean up logging context
     */
    private void cleanupLoggingContext() {
        MDC.clear();
        LoggingContext.clear();
    }
} 