package io.strategiz.framework.exception;

import org.springframework.http.HttpStatus;

/**
 * Utility class to map ErrorCodes to HTTP status codes. Provides a consistent mapping from.
 * internal. error codes to HTTP response codes.
 */
public class ErrorCodeMapper {

  /**
   * Map an ErrorCode to the appropriate HTTP status.
   *
   * @param errorCode the internal error code.
   * @return the corresponding HTTP status.
   */
  public static HttpStatus getHttpStatus(ErrorCode errorCode) {
    if (errorCode == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    return switch (errorCode) {
      case AUTHENTICATION_ERROR -> HttpStatus.UNAUTHORIZED;
      case AUTHORIZATION_ERROR -> HttpStatus.FORBIDDEN;
      case VALIDATION_ERROR, BUSINESS_RULE_VIOLATION, DATA_INTEGRITY_ERROR ->
          HttpStatus.BAD_REQUEST;
      case CONFLICT -> HttpStatus.CONFLICT;
      case EXTERNAL_SERVICE_ERROR -> HttpStatus.BAD_GATEWAY;
      case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
      case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
      case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
      case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
      case NOT_IMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
      case PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
