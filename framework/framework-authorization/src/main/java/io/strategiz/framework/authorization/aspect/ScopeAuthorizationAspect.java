package io.strategiz.framework.authorization.aspect;

import io.strategiz.framework.authorization.annotation.RequireScope;
import io.strategiz.framework.authorization.annotation.ScopeMode;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.context.SecurityContextHolder;
import io.strategiz.framework.authorization.error.AuthorizationErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect that enforces {@link RequireScope} annotation on controller methods.
 *
 * <p>This provides Layer 1 authorization - fast, local scope checking from the PASETO token.
 * No external calls are made.</p>
 *
 * <p>Order: 100 (runs after authentication check, before FGA check)</p>
 */
@Aspect
@Component
@Order(100)
public class ScopeAuthorizationAspect {

    private static final Logger log = LoggerFactory.getLogger(ScopeAuthorizationAspect.class);
    private static final String MODULE_NAME = "authorization";

    @Before("@annotation(requireScope)")
    public void checkScope(JoinPoint joinPoint, RequireScope requireScope) {
        AuthenticatedUser user = SecurityContextHolder.requireAuthenticatedUser();

        String[] requiredScopes = requireScope.value();
        ScopeMode mode = requireScope.mode();

        boolean hasScope = switch (mode) {
            case ANY -> user.hasAnyScope(requiredScopes);
            case ALL -> user.hasAllScopes(requiredScopes);
        };

        if (!hasScope) {
            // Log details for debugging, but don't expose in error message
            log.warn("Authorization failed: user={} missing scopes={} (has={}, mode={})",
                    user.getUserId(),
                    Arrays.toString(requiredScopes),
                    user.getScopes(),
                    mode);
            throw new StrategizException(
                    AuthorizationErrorDetails.INSUFFICIENT_PERMISSIONS,
                    MODULE_NAME
            );
        }

        log.debug("Scope check passed: user={} required={} mode={}",
                user.getUserId(), Arrays.toString(requiredScopes), mode);
    }

    @Before("@within(requireScope)")
    public void checkScopeOnClass(JoinPoint joinPoint, RequireScope requireScope) {
        checkScope(joinPoint, requireScope);
    }
}
