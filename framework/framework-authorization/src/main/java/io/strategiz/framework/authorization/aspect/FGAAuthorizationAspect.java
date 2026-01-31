package io.strategiz.framework.authorization.aspect;

import io.strategiz.framework.authorization.annotation.Authorize;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.context.SecurityContextHolder;
import io.strategiz.framework.authorization.error.AuthorizationErrorDetails;
import io.strategiz.framework.authorization.fga.FGAClient;
import io.strategiz.framework.authorization.model.ResourceType;
import io.strategiz.framework.exception.StrategizException;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Aspect that enforces {@link Authorize} annotation on controller methods.
 *
 * <p>
 * This provides Layer 2 authorization - relationship-based access control via FGA
 * (OpenFGA). It checks if the authenticated user has a specific relation to a resource.
 *
 * <p>
 * Order: 200 (runs after authentication and scope checks)
 */
@Aspect
@Component
@Order(200)
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class FGAAuthorizationAspect {

	private static final Logger log = LoggerFactory.getLogger(FGAAuthorizationAspect.class);

	private static final String MODULE_NAME = "authorization";

	private final FGAClient fgaClient;

	private final ExpressionParser expressionParser = new SpelExpressionParser();

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	/**
	 * Creates a new FGAAuthorizationAspect.
	 * @param fgaClient the FGA client
	 */
	public FGAAuthorizationAspect(FGAClient fgaClient) {
		this.fgaClient = fgaClient;
	}

	/**
	 * Checks authorization before method execution.
	 * @param joinPoint the join point
	 * @param authorize the authorize annotation
	 */
	@Before("@annotation(authorize)")
	public void checkAuthorization(JoinPoint joinPoint, Authorize authorize) {
		AuthenticatedUser user = SecurityContextHolder.requireAuthenticatedUser();

		// Extract resource ID using SpEL
		String resourceId = evaluateSpel(authorize.resourceId(), joinPoint);
		if (resourceId == null || resourceId.isBlank()) {
			log.error("Could not evaluate resourceId expression: {}", authorize.resourceId());
			throw new StrategizException(AuthorizationErrorDetails.ACCESS_DENIED, MODULE_NAME);
		}

		ResourceType resourceType = authorize.resource();
		String relation = authorize.relation();

		// Build FGA identifiers
		String fgaUser = "user:" + user.getUserId();
		String fgaObject = resourceType.getTypeName() + ":" + resourceId;

		// Check authorization
		boolean allowed = fgaClient.check(fgaUser, relation, fgaObject);

		if (!allowed) {
			// Log details for debugging, but don't expose in error message
			log.warn("FGA check failed: user={} relation={} resource={}:{}", user.getUserId(), relation,
					resourceType.getTypeName(), resourceId);
			throw new StrategizException(AuthorizationErrorDetails.ACCESS_DENIED, MODULE_NAME);
		}

		log.debug("FGA check passed: user={} relation={} resource={}:{}", user.getUserId(), relation,
				resourceType.getTypeName(), resourceId);
	}

	/**
	 * Evaluate a SpEL expression against the method parameters.
	 * @param expression the SpEL expression (e.g., "#portfolioId", "#request.id")
	 * @param joinPoint the join point
	 * @return the evaluated value as a string
	 */
	@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
	private String evaluateSpel(String expression, JoinPoint joinPoint) {
		try {
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Method method = signature.getMethod();
			Object[] args = joinPoint.getArgs();

			String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
			if (parameterNames == null) {
				log.error("Could not discover parameter names for method: {}", method.getName());
				return null;
			}

			EvaluationContext context = new StandardEvaluationContext();
			for (int i = 0; i < parameterNames.length; i++) {
				((StandardEvaluationContext) context).setVariable(parameterNames[i], args[i]);
			}

			Object result = expressionParser.parseExpression(expression).getValue(context);
			return result != null ? result.toString() : null;
		}
		catch (Exception e) {
			log.error("Error evaluating SpEL expression '{}': {}", expression, e.getMessage());
			return null;
		}
	}

}
