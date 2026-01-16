package io.strategiz.framework.authorization.aspect;

import io.strategiz.framework.authorization.annotation.RequireAuth;
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

/**
 * Aspect that enforces {@link RequireAuth} annotation on controller methods.
 *
 * <p>Checks:
 *
 * <ul>
 *   <li>User is authenticated
 *   <li>User meets minimum ACR level
 *   <li>User is not in demo mode (if restricted)
 * </ul>
 *
 * <p>Order: 50 (runs before scope and FGA checks)
 */
@Aspect
@Component
@Order(50)
public class AuthenticationAspect {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationAspect.class);

  private static final String MODULE_NAME = "authorization";

  /**
   * Checks authentication requirements before method execution.
   *
   * @param joinPoint the join point
   * @param requireAuth the require auth annotation
   */
  @Before("@annotation(requireAuth)")
  public void checkAuthentication(JoinPoint joinPoint, RequireAuth requireAuth) {
    // If authentication is not required, skip all checks
    if (!requireAuth.required()) {
      log.debug(
          "Authentication optional for {}.{}, skipping checks",
          joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName());
      return;
    }

    // Check if user is authenticated
    AuthenticatedUser user =
        SecurityContextHolder.getAuthenticatedUser()
            .orElseThrow(
                () -> {
                  log.warn(
                      "Authentication required for {}.{}",
                      joinPoint.getSignature().getDeclaringTypeName(),
                      joinPoint.getSignature().getName());
                  return new StrategizException(
                      AuthorizationErrorDetails.NOT_AUTHENTICATED, MODULE_NAME);
                });

    // Check minimum ACR level
    String requiredAcr = requireAuth.minAcr();
    if (!user.meetsMinAcr(requiredAcr)) {
      log.warn(
          "Auth level required: user={} has acr={} but needs acr>={}",
          user.getUserId(),
          user.getAcr(),
          requiredAcr);
      throw new StrategizException(AuthorizationErrorDetails.AUTH_LEVEL_REQUIRED, MODULE_NAME);
    }

    // Check demo mode restriction
    if (!requireAuth.allowDemoMode() && user.isDemoMode()) {
      log.warn(
          "Demo mode restricted: user={} attempted action requiring live mode", user.getUserId());
      throw new StrategizException(AuthorizationErrorDetails.DEMO_MODE_RESTRICTED, MODULE_NAME);
    }

    log.debug(
        "Authentication check passed: user={} acr={} demoMode={}",
        user.getUserId(),
        user.getAcr(),
        user.isDemoMode());
  }

  /**
   * Checks authentication requirements at class level.
   *
   * @param joinPoint the join point
   * @param requireAuth the require auth annotation
   */
  @Before("@within(requireAuth)")
  public void checkAuthenticationOnClass(JoinPoint joinPoint, RequireAuth requireAuth) {
    // Note: Method-level @RequireAuth should override class-level annotation
    // The checkAuthentication method will handle the `required` attribute
    checkAuthentication(joinPoint, requireAuth);
  }

}
