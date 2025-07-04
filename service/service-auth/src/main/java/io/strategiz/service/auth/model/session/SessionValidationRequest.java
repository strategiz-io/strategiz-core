package io.strategiz.service.auth.model.session;

import jakarta.validation.constraints.NotBlank;

/**
 * Request model for validating a session
 * Contains the access token to be validated
 */
public record SessionValidationRequest(
    @NotBlank(message = "Access token is required")
    String accessToken
) {
} 