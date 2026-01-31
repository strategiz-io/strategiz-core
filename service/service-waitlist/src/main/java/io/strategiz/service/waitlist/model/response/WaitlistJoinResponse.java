package io.strategiz.service.waitlist.model.response;

/**
 * Response model for waitlist join operation
 */
public class WaitlistJoinResponse {

	private boolean success;

	private boolean alreadyJoined;

	private String message;

	// Constructors
	public WaitlistJoinResponse() {
	}

	public WaitlistJoinResponse(boolean success, boolean alreadyJoined, String message) {
		this.success = success;
		this.alreadyJoined = alreadyJoined;
		this.message = message;
	}

	// Factory methods
	public static WaitlistJoinResponse success() {
		return new WaitlistJoinResponse(true, false, "Welcome! Check your email for confirmation.");
	}

	public static WaitlistJoinResponse alreadyJoined() {
		return new WaitlistJoinResponse(false, true, "You're already on the waitlist!");
	}

	public static WaitlistJoinResponse error(String message) {
		return new WaitlistJoinResponse(false, false, message);
	}

	// Getters and Setters
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isAlreadyJoined() {
		return alreadyJoined;
	}

	public void setAlreadyJoined(boolean alreadyJoined) {
		this.alreadyJoined = alreadyJoined;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "WaitlistJoinResponse{" + "success=" + success + ", alreadyJoined=" + alreadyJoined + ", message='"
				+ message + '\'' + '}';
	}

}
