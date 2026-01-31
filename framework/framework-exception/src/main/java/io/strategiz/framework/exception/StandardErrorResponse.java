package io.strategiz.framework.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * FINAL standardized error response format for all Strategiz API endpoints.
 *
 * <p>
 * This is the ONLY error response format allowed across the entire application. No
 * additions,. no. modifications, no domain-specific extensions permitted.
 *
 * <p>
 * Contains exactly 4 fields - no more, no less.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardErrorResponse {

	/** Short, uppercase error code (e.g., "AUTHENTICATION_FAILED"). */
	private final String code;

	/** User-friendly error message (e.g., "Invalid credentials provided"). */
	private final String message;

	/**
	 * Technical details for developers (e.g., "Facebook OAuth returned error:
	 * invalid_grant").
	 */
	private final String developerMessage;

	/**
	 * URL to error documentation (e.g.,
	 * "https://docs.strategiz.io/errors/authentication").
	 */
	private final String moreInfo;

	/** Create a standard error response with all required fields. */
	public StandardErrorResponse(String code, String message, String developerMessage, String moreInfo) {
		this.code = code;
		this.message = message;
		this.developerMessage = developerMessage;
		this.moreInfo = moreInfo;
	}

	/**
	 * Static factory method for creating error responses.
	 * @param code Error code.
	 * @param message User message.
	 * @param developerMessage Developer message.
	 * @param moreInfo Documentation URL.
	 * @return New StandardErrorResponse.
	 */
	public static StandardErrorResponse of(String code, String message, String developerMessage, String moreInfo) {
		return new StandardErrorResponse(code, message, developerMessage, moreInfo);
	}

	// Getters only - immutable response
	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public String getDeveloperMessage() {
		return developerMessage;
	}

	public String getMoreInfo() {
		return moreInfo;
	}

	@Override
	public String toString() {
		return "StandardErrorResponse{" + "code='" + code + '\'' + ", message='" + message + '\''
				+ ", developerMessage='" + developerMessage + '\'' + ", moreInfo='" + moreInfo + '\'' + '}';
	}

}
