package io.strategiz.service.auth.model.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Response model for current user information from session token
 */
public record CurrentUserResponse(boolean authenticated, String id, String email, String name, Long createdAt,
		String role) {

	/**
	 * Full constructor with authenticated flag defaulting to true
	 */
	public CurrentUserResponse(String id, String email, String name, Long createdAt, String role) {
		this(true, id, email, name, createdAt, role);
	}

	/**
	 * Constructor without role for backwards compatibility
	 */
	public CurrentUserResponse(String id, String email, String name, Long createdAt) {
		this(true, id, email, name, createdAt, null);
	}

	/**
	 * Factory method for unauthenticated response
	 */
	public static CurrentUserResponse notAuthenticated() {
		return new CurrentUserResponse(false, null, null, null, null, null);
	}

}