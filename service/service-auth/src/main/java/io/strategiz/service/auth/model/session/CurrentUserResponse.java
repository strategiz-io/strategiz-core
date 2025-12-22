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
    @NotNull Long createdAt,
    String role
) {
    /**
     * Constructor without role for backwards compatibility
     */
    public CurrentUserResponse(String id, String email, String name, Long createdAt) {
        this(id, email, name, createdAt, null);
    }
}