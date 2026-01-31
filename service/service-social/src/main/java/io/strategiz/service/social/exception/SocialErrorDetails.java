package io.strategiz.service.social.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for social service.
 */
public enum SocialErrorDetails implements ErrorDetails {

	// Follow errors
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "social-user-not-found"),
	CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "social-cannot-follow-self"),
	ALREADY_FOLLOWING(HttpStatus.CONFLICT, "social-already-following"),
	NOT_FOLLOWING(HttpStatus.NOT_FOUND, "social-not-following"),
	FOLLOW_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "social-follow-failed"),
	UNFOLLOW_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "social-unfollow-failed"),

	// Activity errors
	ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "social-activity-not-found"),
	ACTIVITY_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "social-activity-retrieval-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	SocialErrorDetails(HttpStatus httpStatus, String propertyKey) {
		this.httpStatus = httpStatus;
		this.propertyKey = propertyKey;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}

}
