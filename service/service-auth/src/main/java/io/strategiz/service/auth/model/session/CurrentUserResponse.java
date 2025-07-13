package io.strategiz.service.auth.model.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Response model for current user information from session token
 */
public record CurrentUserResponse(
    @NotBlank String id,
    @NotBlank String email,
    @NotBlank String name,
    @NotNull Long createdAt
) {}