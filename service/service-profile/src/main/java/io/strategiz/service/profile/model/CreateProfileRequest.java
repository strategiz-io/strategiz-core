package io.strategiz.service.profile.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for Step 1 of signup: Profile Creation
 *
 * Contains only basic profile information. No authentication method details.
 */
public class CreateProfileRequest {

	@NotBlank(message = "Name is required")
	@Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
	private String name;

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	private String email;

	private String photoURL;

	private Boolean demoMode;

	// Default constructor
	public CreateProfileRequest() {
	}

	// Constructor
	public CreateProfileRequest(String name, String email, String photoURL, Boolean demoMode) {
		this.name = name;
		this.email = email;
		this.photoURL = photoURL;
		this.demoMode = demoMode;
	}

	// Getters and setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(String photoURL) {
		this.photoURL = photoURL;
	}

	public Boolean getDemoMode() {
		return demoMode;
	}

	public void setDemoMode(Boolean demoMode) {
		this.demoMode = demoMode;
	}

	@Override
	public String toString() {
		return "CreateProfileRequest{" + "name='" + name + '\'' + ", email='" + email + '\'' + ", photoURL='" + photoURL
				+ '\'' + ", demoMode=" + demoMode + '}';
	}

}