package io.strategiz.service.auth.model.session;

/**
 * Response model for session revocation operation Contains the result of the revocation
 * attempt
 */
public record RevocationResponse(boolean revoked, String message) {
}