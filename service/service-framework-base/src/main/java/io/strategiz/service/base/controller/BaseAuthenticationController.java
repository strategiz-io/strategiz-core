package io.strategiz.service.base.controller;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;

import java.security.Principal;

/**
 * Base controller for authentication-related REST controllers in the Strategiz application.
 * Extends BaseController with authentication-specific functionality.
 * 
 * Authentication controllers should:
 * - Extend this class instead of BaseController directly
 * - Use provided user extraction and role validation methods
 * - Override role validation logic as needed for specific auth flows
 * - Throw StrategizException for authentication/authorization errors
 */
public abstract class BaseAuthenticationController extends BaseController {
    
    /**
     * Extract user ID from authentication principal
     * @param principal The authentication principal from the security context
     * @return The user ID extracted from the principal
     * @throws StrategizException if principal is null in production environment
     */
    protected String extractUserId(Principal principal) {
        if (principal != null) {
            String userId = principal.getName();
            log.debug("Extracted user ID from principal: {}", userId);
            return userId;
        }
        
        // For development/testing - should not happen in production
        log.warn("No principal found, using test user ID");
        return "test-user-" + System.currentTimeMillis();
    }
    
    /**
     * Extract user ID from authentication principal with role validation
     * @param principal The authentication principal from the security context
     * @param requiredRoles The roles required to access the resource
     * @return The user ID extracted from the principal
     * @throws StrategizException if authentication fails or roles are insufficient
     */
    protected String extractUserIdWithRoleCheck(Principal principal, String... requiredRoles) {
        String userId = extractUserId(principal);
        
        if (requiredRoles.length > 0) {
            validateUserRoles(userId, requiredRoles);
        }
        
        return userId;
    }
    
    /**
     * Validate that the user has the required roles
     * Default implementation - override in subclasses with actual role checking logic
     * 
     * @param userId The user ID to validate roles for
     * @param requiredRoles The roles required for the operation
     * @throws StrategizException if user doesn't have required roles
     */
    protected void validateUserRoles(String userId, String... requiredRoles) {
        // Default implementation - override in subclasses with actual role checking
        log.debug("Role validation requested for user: {}, roles: {}", userId, String.join(",", requiredRoles));
        
        // Example implementation:
        // if (!userHasAnyRole(userId, requiredRoles)) {
        //     throw new StrategizException(ErrorCode.AUTHORIZATION_ERROR, 
        //         "User " + userId + " lacks required roles: " + String.join(",", requiredRoles));
        // }
    }
    
    /**
     * Validate that the user is authenticated
     * @param principal The authentication principal
     * @throws StrategizException if user is not authenticated
     */
    protected void validateAuthenticated(Principal principal) {
        if (principal == null) {
            throw new StrategizException(ErrorCode.AUTHENTICATION_ERROR, 
                "Authentication required for this operation");
        }
    }
    
    /**
     * Validate that the user has access to a specific resource
     * Default implementation - override in subclasses with actual access control logic
     * 
     * @param userId The user requesting access
     * @param resourceType The type of resource being accessed
     * @param resourceId The ID of the specific resource
     * @throws StrategizException if user doesn't have access to the resource
     */
    protected void validateResourceAccess(String userId, String resourceType, String resourceId) {
        log.debug("Resource access validation for user: {}, resource: {}:{}", 
            userId, resourceType, resourceId);
        
        // Default implementation - override in subclasses with actual access control
        // Example implementation:
        // if (!userHasAccessToResource(userId, resourceType, resourceId)) {
        //     throw new StrategizException(ErrorCode.AUTHORIZATION_ERROR, 
        //         "User " + userId + " does not have access to " + resourceType + ":" + resourceId);
        // }
    }
    
    /**
     * Validate that the user owns a specific resource
     * @param userId The user ID to check ownership for
     * @param resourceOwnerId The owner ID of the resource
     * @throws StrategizException if user doesn't own the resource
     */
    protected void validateResourceOwnership(String userId, String resourceOwnerId) {
        if (!userId.equals(resourceOwnerId)) {
            throw new StrategizException(ErrorCode.AUTHORIZATION_ERROR, 
                "User " + userId + " does not own this resource");
        }
    }
    
    /**
     * Check if user has admin privileges
     * Default implementation - override in subclasses with actual admin checking
     * 
     * @param userId The user ID to check admin status for
     * @return true if user has admin privileges, false otherwise
     */
    protected boolean isAdmin(String userId) {
        // Default implementation - override in subclasses
        log.debug("Admin check for user: {}", userId);
        return false;
    }
    
    /**
     * Require admin privileges for the current operation
     * @param userId The user ID to check admin status for
     * @throws StrategizException if user is not an admin
     */
    protected void requireAdmin(String userId) {
        if (!isAdmin(userId)) {
            throw new StrategizException(ErrorCode.AUTHORIZATION_ERROR, 
                "Admin privileges required for this operation");
        }
    }
    
    /**
     * Log authentication event for security monitoring
     * @param userId The user ID involved in the event
     * @param event The type of authentication event
     * @param details Additional details about the event
     * @param success Whether the event was successful
     */
    protected void logAuthenticationEvent(String userId, String event, String details, boolean success) {
        if (success) {
            log.info("AUTH_SUCCESS - User: {}, Event: {}, Details: {}", userId, event, details);
        } else {
            log.warn("AUTH_FAILURE - User: {}, Event: {}, Details: {}", userId, event, details);
        }
    }
    
    /**
     * Log authorization event for security monitoring
     * @param userId The user ID involved in the event
     * @param resource The resource being accessed
     * @param action The action being performed
     * @param success Whether the authorization was successful
     */
    protected void logAuthorizationEvent(String userId, String resource, String action, boolean success) {
        if (success) {
            log.info("AUTHZ_SUCCESS - User: {}, Resource: {}, Action: {}", userId, resource, action);
        } else {
            log.warn("AUTHZ_FAILURE - User: {}, Resource: {}, Action: {}", userId, resource, action);
        }
    }
} 