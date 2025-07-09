package io.strategiz.framework.apidocs.annotation;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to standardize API documentation across all controllers.
 * This provides consistent OpenAPI documentation for all endpoints.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@SecurityRequirement(name = "bearer-jwt")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successful operation"),
    @ApiResponse(responseCode = "400", description = "Bad request - Invalid input parameters"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
    @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
    @ApiResponse(responseCode = "404", description = "Resource not found"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
})
public @interface ApiDocumented {
    /**
     * Short summary of what the operation does
     */
    String summary() default "";
    
    /**
     * Detailed description of the operation
     */
    String description() default "";
    
    /**
     * API tags for grouping endpoints
     */
    String[] tags() default {};
}
