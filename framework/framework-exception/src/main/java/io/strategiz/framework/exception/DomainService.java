package io.strategiz.framework.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate a service belongs to a specific domain. This is used for categorizing.
 * services and associating exceptions with specific domains.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainService {
  /** The domain name this service belongs to. */
  String domain();
}
