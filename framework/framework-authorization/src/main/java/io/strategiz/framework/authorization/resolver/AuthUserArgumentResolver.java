package io.strategiz.framework.authorization.resolver;

import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.context.SecurityContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolver for {@link AuthUser} annotated method parameters.
 *
 * <p>Injects the current {@link AuthenticatedUser} from the security context into controller method
 * parameters.
 *
 * <p>Usage:
 *
 * <pre>
 * &#64;GetMapping("/profile")
 * public ResponseEntity&lt;?&gt; getProfile(&#64;AuthUser AuthenticatedUser user) {
 *     // user is guaranteed to be non-null
 * }
 *
 * &#64;GetMapping("/public")
 * public ResponseEntity&lt;?&gt; publicEndpoint(&#64;AuthUser(required = false) AuthenticatedUser user) {
 *     // user may be null if not authenticated
 * }
 * </pre>
 */
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

  private static final String MODULE_NAME = "authorization";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(AuthUser.class)
        && AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    AuthUser annotation = parameter.getParameterAnnotation(AuthUser.class);
    if (annotation == null) {
      return null;
    }

    if (annotation.required()) {
      return SecurityContextHolder.requireAuthenticatedUser();
    } else {
      return SecurityContextHolder.getAuthenticatedUser().orElse(null);
    }
  }
}
