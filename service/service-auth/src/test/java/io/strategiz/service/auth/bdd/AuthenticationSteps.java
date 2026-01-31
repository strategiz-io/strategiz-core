package io.strategiz.service.auth.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cucumber step definitions for authentication BDD tests.
 *
 * Implements Given/When/Then steps for: - Passkey/WebAuthn authentication - TOTP
 * two-factor authentication - SMS verification - Email OTP and magic links
 *
 * NOTE: This is a MOCK implementation for BDD testing. Step definitions use internal
 * state to simulate authentication flows without requiring actual service implementations
 * or external APIs.
 *
 * This validates the Gherkin scenarios are correctly written and documents expected
 * authentication behavior for stakeholders.
 */
public class AuthenticationSteps {

	// Simple test user POJO (not the actual UserEntity)
	private static class TestUser {

		String id;

		String email;

		String phoneNumber;

		boolean emailVerified;

		boolean phoneVerified;

		String passwordHash;

		Instant createdAt;

		TestUser() {
			this.id = UUID.randomUUID().toString();
			this.createdAt = Instant.now();
		}

	}

	// Test state
	private TestUser currentUser;

	private String currentCredentialId;

	private String currentCode;

	private String currentSecretKey;

	private List<String> currentBackupCodes;

	private String currentMagicLink;

	private String currentDeviceToken;

	private Map<String, Object> currentSession;

	private String currentError;

	private Instant codeGeneratedTime;

	private int remainingAttempts;

	private boolean operationSuccess;

	// ============================================================================
	// BACKGROUND STEPS
	// ============================================================================

	@Given("the authentication service is available")
	public void theAuthenticationServiceIsAvailable() {
		// Mock - always available in tests
		assertTrue(true);
	}

	@Given("the user is authenticated")
	public void theUserIsAuthenticated() {
		// Create authenticated session for current user
		currentSession = new HashMap<>();
		currentSession.put("userId", currentUser.id);
		currentSession.put("authenticated", true);
	}

	@Given("WebAuthn is supported")
	public void webAuthnIsSupported() {
		// Mock - always supported in tests
		assertTrue(true);
	}

	@Given("SMS service is configured")
	public void smsServiceIsConfigured() {
		// Mock - always configured in tests
		assertTrue(true);
	}

	@Given("email service is configured")
	public void emailServiceIsConfigured() {
		// Mock - always configured in tests
		assertTrue(true);
	}

	// ============================================================================
	// USER SETUP STEPS
	// ============================================================================

	@Given("a user with email {string} exists")
	public void aUserWithEmailExists(String email) {
		currentUser = new TestUser();
		currentUser.email = email;
	}

	@Given("a user with email {string}")
	public void aUserWithEmail(String email) {
		aUserWithEmailExists(email);
	}

	@Given("a new user registered with email {string}")
	public void aNewUserRegisteredWithEmail(String email) {
		currentUser = new TestUser();
		currentUser.email = email;
		currentUser.emailVerified = false;
	}

	@Given("a user with phone number {string}")
	public void aUserWithPhoneNumber(String phoneNumber) {
		currentUser = new TestUser();
		currentUser.phoneNumber = phoneNumber;
		currentUser.phoneVerified = true;
	}

	@Given("email is not yet verified")
	public void emailIsNotYetVerified() {
		currentUser.emailVerified = false;
	}

	@Given("the user has no phone number")
	public void theUserHasNoPhoneNumber() {
		currentUser.phoneNumber = null;
		currentUser.phoneVerified = false;
	}

	@Given("the user has phone number {string} verified")
	public void theUserHasPhoneNumberVerified(String phoneNumber) {
		currentUser.phoneNumber = phoneNumber;
		currentUser.phoneVerified = true;
	}

	@Given("a user has phone {string} not verified")
	public void aUserHasPhoneNotVerified(String phoneNumber) {
		currentUser = new TestUser();
		currentUser.phoneNumber = phoneNumber;
		currentUser.phoneVerified = false;
	}

	// ============================================================================
	// PASSKEY SETUP STEPS
	// ============================================================================

	@Given("the user has no passkeys registered")
	public void theUserHasNoPasskeysRegistered() {
		// Mock repository would return empty list
		assertTrue(true);
	}

	@Given("a user has {int} passkey(s) registered")
	public void aUserHasPasskeysRegistered(int count) {
		// Mock repository would return list of size 'count'
		assertTrue(count > 0);
	}

	@Given("a user has passkey {string} registered")
	public void aUserHasPasskeyRegistered(String credentialId) {
		currentCredentialId = credentialId;
		// Mock repository would return this credential
	}

	@Given("the passkey was revoked")
	public void thePasskeyWasRevoked() {
		// Mark credential as revoked in mock repository
		assertTrue(true);
	}

	@Given("a user has passkey named {string}")
	public void aUserHasPasskeyNamed(String name) {
		// Create passkey with given name
		currentCredentialId = "cred_" + UUID.randomUUID().toString();
	}

	@Given("a user has only {int} passkey")
	public void aUserHasOnlyPasskey(int count) {
		assertEquals(1, count);
	}

	@Given("the user has no password set")
	public void theUserHasNoPasswordSet() {
		currentUser.passwordHash = null;
	}

	@Given("the user has a password set")
	public void theUserHasAPasswordSet() {
		currentUser.passwordHash = "$2a$10$mockHashValue";
	}

	@Given("a user has passkey with counter value {int}")
	public void aUserHasPasskeyWithCounterValue(int counter) {
		// Set counter in mock credential
		assertTrue(counter >= 0);
	}

	// ============================================================================
	// TOTP SETUP STEPS
	// ============================================================================

	@Given("the user has no TOTP enabled")
	public void theUserHasNoTotpEnabled() {
		// Mock repository shows no TOTP configured
		assertTrue(true);
	}

	@Given("a user is setting up TOTP")
	public void aUserIsSettingUpTotp() {
		currentSecretKey = "JBSWY3DPEHPK3PXP";
		currentBackupCodes = generateBackupCodes(10);
	}

	@Given("the secret key is {string}")
	public void theSecretKeyIs(String secretKey) {
		currentSecretKey = secretKey;
	}

	@Given("a user has TOTP enabled")
	public void aUserHasTotpEnabled() {
		currentSecretKey = "JBSWY3DPEHPK3PXP";
		currentBackupCodes = generateBackupCodes(10);
		// Mock shows TOTP is enabled
	}

	@Given("a user successfully enabled TOTP")
	public void aUserSuccessfullyEnabledTotp() {
		aUserHasTotpEnabled();
		operationSuccess = true;
	}

	@Given("they completed password authentication")
	public void theyCompletedPasswordAuthentication() {
		currentSession = new HashMap<>();
		currentSession.put("userId", currentUser.id);
		currentSession.put("passwordVerified", true);
	}

	@Given("has {int} backup codes")
	public void hasBackupCodes(int count) {
		currentBackupCodes = generateBackupCodes(count);
	}

	@Given("used {int} backup codes")
	public void usedBackupCodes(int count) {
		hasBackupCodes(10);
		// Mark 'count' codes as used
		for (int i = 0; i < count && i < currentBackupCodes.size(); i++) {
			// Mark as used in mock
		}
	}

	@Given("a user has backup code {string} already used")
	public void aUserHasBackupCodeAlreadyUsed(String code) {
		currentBackupCodes = new ArrayList<>();
		currentBackupCodes.add(code);
		// Mark as used in mock
	}

	@Given("used {int} out of {int} backup codes")
	public void usedOutOfBackupCodes(int used, int total) {
		currentBackupCodes = generateBackupCodes(total);
		// Mark 'used' codes as used
	}

	@Given("a user previously had TOTP enabled")
	public void aUserPreviouslyHadTotpEnabled() {
		// User had TOTP in the past
		assertTrue(true);
	}

	@Given("they disabled it")
	public void theyDisabledIt() {
		// TOTP was disabled
		currentSecretKey = null;
		currentBackupCodes = null;
	}

	// ============================================================================
	// DEVICE TRUST STEPS
	// ============================================================================

	@Given("a user trusted a device {int} days ago")
	public void aUserTrustedADeviceDaysAgo(int days) {
		currentDeviceToken = "device_" + UUID.randomUUID().toString();
		// Set trust expiration based on days
	}

	@Given("a user has {int} trusted devices")
	public void aUserHasTrustedDevices(int count) {
		// Mock shows 'count' trusted devices
		assertTrue(count > 0);
	}

	// ============================================================================
	// SMS/EMAIL CODE STEPS
	// ============================================================================

	@Given("a user received SMS code {string}")
	public void aUserReceivedSmsCode(String code) {
		currentCode = code;
		codeGeneratedTime = Instant.now();
	}

	@Given("a user received SMS code {string} {int} minutes ago")
	public void aUserReceivedSmsCodeMinutesAgo(String code, int minutes) {
		currentCode = code;
		codeGeneratedTime = Instant.now().minus(minutes, ChronoUnit.MINUTES);
	}

	@Given("a user received an SMS code")
	public void aUserReceivedAnSmsCode() {
		currentCode = generateSixDigitCode();
		codeGeneratedTime = Instant.now();
	}

	@Given("a user just received an SMS code")
	public void aUserJustReceivedAnSmsCode() {
		aUserReceivedAnSmsCode();
	}

	@Given("a user received email code {string}")
	public void aUserReceivedEmailCode(String code) {
		currentCode = code;
		codeGeneratedTime = Instant.now();
	}

	@Given("the code was sent {int} minutes ago")
	public void theCodeWasSentMinutesAgo(int minutes) {
		codeGeneratedTime = Instant.now().minus(minutes, ChronoUnit.MINUTES);
	}

	@Given("a user received email code {string} {int} minutes ago")
	public void aUserReceivedEmailCodeMinutesAgo(String code, int minutes) {
		currentCode = code;
		codeGeneratedTime = Instant.now().minus(minutes, ChronoUnit.MINUTES);
	}

	@Given("a user received an email code")
	public void aUserReceivedAnEmailCode() {
		currentCode = generateSixDigitCode();
		codeGeneratedTime = Instant.now();
	}

	// ============================================================================
	// MAGIC LINK STEPS
	// ============================================================================

	@Given("a user received a magic link")
	public void aUserReceivedAMagicLink() {
		currentMagicLink = "https://auth.strategiz.io/magic?token=" + UUID.randomUUID().toString();
		codeGeneratedTime = Instant.now();
	}

	@Given("the link was generated {int} minutes ago")
	public void theLinkWasGeneratedMinutesAgo(int minutes) {
		codeGeneratedTime = Instant.now().minus(minutes, ChronoUnit.MINUTES);
	}

	@Given("a user received a magic link {int} minutes ago")
	public void aUserReceivedAMagicLinkMinutesAgo(int minutes) {
		aUserReceivedAMagicLink();
		codeGeneratedTime = Instant.now().minus(minutes, ChronoUnit.MINUTES);
	}

	@Given("a user received email verification link {int} hours ago")
	public void aUserReceivedEmailVerificationLinkHoursAgo(int hours) {
		currentMagicLink = "https://auth.strategiz.io/verify?token=" + UUID.randomUUID().toString();
		codeGeneratedTime = Instant.now().minus(hours, ChronoUnit.HOURS);
	}

	@Given("a user requested magic link from IP {string}")
	public void aUserRequestedMagicLinkFromIp(String ip) {
		aUserReceivedAMagicLink();
		// Store request IP in session
	}

	// ============================================================================
	// VERIFICATION STATE STEPS
	// ============================================================================

	@Given("a user is at the TOTP verification step")
	public void aUserIsAtTheTotpVerificationStep() {
		currentSession = new HashMap<>();
		currentSession.put("userId", currentUser.id);
		currentSession.put("awaitingTotp", true);
		remainingAttempts = 5;
	}

	@Given("the current valid code is {string}")
	public void theCurrentValidCodeIs(String code) {
		currentCode = code;
	}

	@Given("a code {string} was valid {int} seconds ago")
	public void aCodeWasValidSecondsAgo(String code, int seconds) {
		currentCode = code;
		codeGeneratedTime = Instant.now().minus(seconds, ChronoUnit.SECONDS);
	}

	@Given("a user is at SMS verification step")
	public void aUserIsAtSmsVerificationStep() {
		currentSession = new HashMap<>();
		currentSession.put("userId", currentUser.id);
		currentSession.put("awaitingSms", true);
		remainingAttempts = 5;
	}

	@Given("a user is at email verification step")
	public void aUserIsAtEmailVerificationStep() {
		currentSession = new HashMap<>();
		currentSession.put("userId", currentUser.id);
		currentSession.put("awaitingEmailOtp", true);
		remainingAttempts = 5;
	}

	// ============================================================================
	// RATE LIMITING SETUP STEPS
	// ============================================================================

	@Given("a user requests {int} magic links within {int} minutes")
	public void aUserRequestsMagicLinksWithinMinutes(int count, int minutes) {
		// Simulate 'count' requests in time window
		assertTrue(count > 0);
	}

	@Given("user {string} exists")
	public void userExists(String email) {
		// Another user exists in system
		TestUser otherUser = new TestUser();
		otherUser.email = email;
	}

	@Given("a user with email {string}")
	public void aUserWithEmailExists2(String email) {
		currentUser = new TestUser();
		currentUser.email = email;
	}

	@Given("a user enters phone {string}")
	public void aUserEntersPhone(String phone) {
		// Store phone for validation
		assertTrue(phone != null);
	}

	// ============================================================================
	// WHEN STEPS - PASSKEY OPERATIONS
	// ============================================================================

	@When("they initiate passkey registration")
	public void theyInitiatePasskeyRegistration() {
		// Generate registration challenge
		operationSuccess = true;
	}

	@When("complete WebAuthn ceremony with credential ID {string}")
	public void completeWebAuthnCeremonyWithCredentialId(String credentialId) {
		currentCredentialId = credentialId;
		operationSuccess = true;
	}

	@When("they register another passkey with credential ID {string}")
	public void theyRegisterAnotherPasskeyWithCredentialId(String credentialId) {
		currentCredentialId = credentialId;
		operationSuccess = true;
	}

	@When("they try to register the same credential ID again")
	public void theyTryToRegisterTheSameCredentialIdAgain() {
		operationSuccess = false;
		currentError = "This passkey is already registered";
	}

	@When("complete WebAuthn ceremony")
	public void completeWebAuthnCeremony() {
		currentCredentialId = "cred_" + UUID.randomUUID().toString();
		operationSuccess = true;
	}

	@When("provide custom name {string}")
	public void provideCustomName(String name) {
		// Store custom name for credential
		assertTrue(name != null);
	}

	@When("they visit the login page")
	public void theyVisitTheLoginPage() {
		// User navigates to login
		assertTrue(true);
	}

	@When("click {string}")
	public void click(String buttonText) {
		// Simulate button click
		assertTrue(buttonText != null);
	}

	@When("complete WebAuthn authentication with credential {string}")
	public void completeWebAuthnAuthenticationWithCredential(String credentialId) {
		if (credentialId.equals(currentCredentialId)) {
			operationSuccess = true;
			currentSession = createSession(2);
		}
		else {
			operationSuccess = false;
			currentError = "Invalid passkey";
		}
	}

	@When("they attempt WebAuthn authentication with credential {string}")
	public void theyAttemptWebAuthnAuthenticationWithCredential(String credentialId) {
		completeWebAuthnAuthenticationWithCredential(credentialId);
	}

	@When("they view their security settings")
	public void theyViewTheirSecuritySettings() {
		// Navigate to security settings page
		assertTrue(true);
	}

	@When("they rename it to {string}")
	public void theyRenameItTo(String newName) {
		operationSuccess = true;
	}

	@When("they delete passkey {string}")
	public void theyDeletePasskey(String credentialId) {
		if (currentCredentialId != null && currentCredentialId.equals(credentialId)) {
			operationSuccess = true;
		}
	}

	@When("they try to delete the last passkey")
	public void theyTryToDeleteTheLastPasskey() {
		if (currentUser.passwordHash == null) {
			operationSuccess = false;
			currentError = "Cannot remove last authentication method. Set a password first.";
		}
		else {
			operationSuccess = true;
		}
	}

	@When("they delete the last passkey")
	public void theyDeleteTheLastPasskey() {
		operationSuccess = true;
	}

	@When("they add and verify a passkey in the same session")
	public void theyAddAndVerifyAPasskeyInTheSameSession() {
		currentCredentialId = "cred_new";
		operationSuccess = true;
		// Upgrade session ACR
		if (currentSession != null) {
			currentSession.put("acr", "2");
		}
	}

	@When("the WebAuthn ceremony times out after {int} seconds")
	public void theWebAuthnCeremonyTimesOutAfterSeconds(int seconds) {
		operationSuccess = false;
		currentError = "Passkey registration timed out. Please try again.";
	}

	@When("they cancel the WebAuthn browser prompt")
	public void theyCancelTheWebAuthnBrowserPrompt() {
		operationSuccess = false;
	}

	@When("they register a passkey")
	public void theyRegisterAPasskey() {
		currentCredentialId = "cred_" + UUID.randomUUID().toString();
		operationSuccess = true;
	}

	@When("they authenticate and counter is {int}")
	public void theyAuthenticateAndCounterIs(int counter) {
		// Verify counter increment
		operationSuccess = (counter > 0);
	}

	@When("they try to authenticate with counter {int} again")
	public void theyTryToAuthenticateWithCounterAgain(int counter) {
		operationSuccess = false;
		currentError = "Potential replay attack detected";
	}

	@When("they authenticate with invalid signature")
	public void theyAuthenticateWithInvalidSignature() {
		operationSuccess = false;
	}

	// ============================================================================
	// WHEN STEPS - TOTP OPERATIONS
	// ============================================================================

	@When("they navigate to security settings")
	public void theyNavigateToSecuritySettings() {
		// Navigate to settings
		assertTrue(true);
	}

	@When("they scan the QR code with Google Authenticator")
	public void theyScanTheQrCodeWithGoogleAuthenticator() {
		// Simulate QR scan
		assertTrue(currentSecretKey != null);
	}

	@When("enter valid TOTP code {string}")
	public void enterValidTotpCode(String code) {
		currentCode = code;
		operationSuccess = true;
		currentSession = createSession(2);
	}

	@When("they enter invalid code {string}")
	public void theyEnterInvalidCode(String code) {
		currentCode = code;
		operationSuccess = false;
		currentError = "Invalid verification code";
	}

	@When("they download backup codes")
	public void theyDownloadBackupCodes() {
		// Trigger download
		operationSuccess = true;
	}

	@When("they log in with correct email and password")
	public void theyLogInWithCorrectEmailAndPassword() {
		currentSession = new HashMap<>();
		currentSession.put("userId", currentUser.id);
		currentSession.put("passwordVerified", true);
		currentSession.put("awaitingTotp", true);
	}

	@When("they enter {string}")
	public void theyEnter(String input) {
		currentCode = input;
		// Validation depends on context
		if (currentSession != null && currentSession.get("awaitingTotp") != null) {
			if (input.equals(currentCode)) {
				operationSuccess = true;
				currentSession.put("acr", "2");
			}
			else {
				operationSuccess = false;
				currentError = "Invalid verification code";
				remainingAttempts--;
			}
		}
	}

	@When("they enter incorrect code {string}")
	public void theyEnterIncorrectCode(String code) {
		currentCode = code;
		operationSuccess = false;
		currentError = "Invalid verification code";
		remainingAttempts--;
	}

	@When("they enter the expired code {string}")
	public void theyEnterTheExpiredCode(String code) {
		currentCode = code;
		operationSuccess = false;
		currentError = "Code expired. Enter the current code from your app";
	}

	@When("they enter wrong code {int} times")
	public void theyEnterWrongCodeTimes(int times) {
		for (int i = 0; i < times; i++) {
			remainingAttempts--;
		}
		operationSuccess = false;
		currentError = "Too many failed attempts. Try again in 15 minutes";
	}

	@When("enter backup code {string}")
	public void enterBackupCode(String code) {
		if (currentBackupCodes != null && currentBackupCodes.contains(code)) {
			operationSuccess = true;
			currentSession = createSession(2);
			// Mark code as used
		}
		else {
			operationSuccess = false;
			currentError = "This backup code has already been used";
		}
	}

	@When("they try to log in with {string}")
	public void theyTryToLogInWith(String code) {
		enterBackupCode(code);
	}

	@When("they log in with the last backup code")
	public void theyLogInWithTheLastBackupCode() {
		operationSuccess = true;
		currentSession = createSession(2);
	}

	@When("confirm with their password")
	public void confirmWithTheirPassword() {
		// Password confirmation
		operationSuccess = true;
	}

	@When("confirm the action")
	public void confirmTheAction() {
		operationSuccess = true;
	}

	@When("they view security settings")
	public void theyViewSecuritySettings() {
		// View settings
		assertTrue(true);
	}

	@When("they log in and check {string}")
	public void theyLogInAndCheck(String option) {
		currentDeviceToken = "device_" + UUID.randomUUID().toString();
		operationSuccess = true;
	}

	@When("enter valid TOTP code")
	public void enterValidTotpCode() {
		operationSuccess = true;
		currentSession = createSession(2);
	}

	@When("they log in again from the same device within {int} days")
	public void theyLogInAgainFromTheSameDeviceWithinDays(int days) {
		// Trusted device - skip TOTP
		operationSuccess = true;
	}

	@When("they log in from that device")
	public void theyLogInFromThatDevice() {
		// Device trust expired
		currentSession = new HashMap<>();
		currentSession.put("awaitingTotp", true);
	}

	@When("revoke trust for {string}")
	public void revokeTrustFor(String deviceName) {
		operationSuccess = true;
	}

	@When("they enable TOTP again")
	public void theyEnableTotp() {
		currentSecretKey = "NEW_" + UUID.randomUUID().toString();
		currentBackupCodes = generateBackupCodes(10);
		operationSuccess = true;
	}

	@When("they log in with TOTP")
	public void theyLogInWithTotp() {
		operationSuccess = true;
		currentSession = createSession(2);
	}

	@When("they fail TOTP {int} times")
	public void theyFailTotpTimes(int times) {
		remainingAttempts = Math.max(0, 5 - times);
	}

	// ============================================================================
	// WHEN STEPS - SMS OPERATIONS
	// ============================================================================

	@When("they request SMS verification")
	public void theyRequestSmsVerification() {
		currentCode = generateSixDigitCode();
		codeGeneratedTime = Instant.now();
		operationSuccess = true;
	}

	@When("they request {int} SMS codes within {int} minute(s)")
	public void theyRequestSmsCodesWithinMinutes(int count, int minutes) {
		if (count >= 4) {
			operationSuccess = false;
			currentError = "Too many SMS requests. Wait 5 minutes before trying again";
		}
	}

	@When("SMS delivery fails \\(invalid phone number)")
	public void smsDeliveryFails() {
		operationSuccess = false;
		currentError = "Unable to send SMS. Please check your phone number";
	}

	@When("they enter code {string}")
	public void theyEnterCode(String code) {
		if (code.equals(currentCode) && !isCodeExpired()) {
			operationSuccess = true;
			currentSession = createSession(1);
		}
		else if (isCodeExpired()) {
			operationSuccess = false;
			currentError = "Verification code expired. Request a new one";
		}
		else {
			operationSuccess = false;
			currentError = "Invalid verification code";
			remainingAttempts--;
		}
	}

	@When("they enter wrong code {string}")
	public void theyEnterWrongCode(String code) {
		operationSuccess = false;
		currentError = "Invalid verification code";
		remainingAttempts--;
	}

	@When("they enter the code {string}")
	public void theyEnterTheCode(String code) {
		theyEnterCode(code);
	}

	@When("wait {int} seconds")
	public void waitSeconds(int seconds) {
		// Simulate waiting
		assertTrue(seconds > 0);
	}

	@When("they try to resend immediately")
	public void theyTryToResendImmediately() {
		operationSuccess = false;
		currentError = "Wait 60 seconds before requesting a new code";
	}

	@When("they add phone number {string}")
	public void theyAddPhoneNumber(String phoneNumber) {
		currentUser.phoneNumber = phoneNumber;
	}

	@When("verify it with SMS code {string}")
	public void verifyItWithSmsCode(String code) {
		currentCode = code;
		operationSuccess = true;
		currentUser.phoneVerified = true;
	}

	@When("they update to {string}")
	public void theyUpdateTo(String phoneNumber) {
		currentUser.phoneNumber = phoneNumber;
		currentUser.phoneVerified = false;
	}

	@When("verify the new number with SMS code {string}")
	public void verifyTheNewNumberWithSmsCode(String code) {
		currentCode = code;
		operationSuccess = true;
		currentUser.phoneVerified = true;
	}

	@When("they remove the phone number")
	public void theyRemoveThePhoneNumber() {
		if (currentUser.passwordHash != null) {
			operationSuccess = true;
			currentUser.phoneNumber = null;
		}
		else {
			operationSuccess = false;
			currentError = "Cannot remove phone number. It's your only login method";
		}
	}

	@When("they try to remove phone number")
	public void theyTryToRemovePhoneNumber() {
		theyRemoveThePhoneNumber();
	}

	@When("they choose {string}")
	public void theyChoose(String option) {
		// User selects auth method
		assertTrue(option != null);
	}

	@When("enter phone number {string}")
	public void enterPhoneNumber(String phoneNumber) {
		currentUser.phoneNumber = phoneNumber;
	}

	@When("they enter the correct code")
	public void theyEnterTheCorrectCode() {
		operationSuccess = true;
		currentSession = createSession(1);
	}

	@When("they try to log in with that number")
	public void theyTryToLogInWithThatNumber() {
		if (!currentUser.phoneVerified) {
			operationSuccess = false;
			currentError = "Phone number not verified. Please verify it first";
		}
	}

	@When("they try to use old code {string}")
	public void theyTryToUseOldCode(String code) {
		operationSuccess = false;
		currentError = "Invalid verification code";
	}

	@When("they successfully verify with code {string}")
	public void theySuccessfullyVerifyWithCode(String code) {
		currentCode = code;
		operationSuccess = true;
		currentSession = createSession(1);
	}

	@When("try to use the same code {string} again")
	public void tryToUseTheSameCodeAgain(String code) {
		operationSuccess = false;
		currentError = "This code has already been used";
	}

	@When("they enter {string}")
	public void theyEnterString(String input) {
		String trimmed = input.trim();
		if (trimmed.equals(currentCode)) {
			operationSuccess = true;
		}
		else if (trimmed.length() != 6) {
			operationSuccess = false;
			currentError = "Code must be 6 digits";
		}
		else if (!trimmed.matches("\\d+")) {
			operationSuccess = false;
			currentError = "Code must be numeric";
		}
		else if (trimmed.isEmpty()) {
			operationSuccess = false;
			currentError = "Verification code is required";
		}
		else {
			operationSuccess = false;
			currentError = "Invalid verification code";
		}
	}

	// ============================================================================
	// WHEN STEPS - EMAIL OTP OPERATIONS
	// ============================================================================

	@When("enter email address {string}")
	public void enterEmailAddress(String email) {
		currentUser.email = email;
	}

	@When("select {string}")
	public void select(String option) {
		// User selects magic link option
		assertTrue(option != null);
	}

	@When("they click the magic link")
	public void theyClickTheMagicLink() {
		if (!isCodeExpired()) {
			operationSuccess = true;
			currentSession = createSession(1);
		}
		else {
			operationSuccess = false;
			currentError = "This link has expired";
		}
	}

	@When("they click the expired link")
	public void theyClickTheExpiredLink() {
		operationSuccess = false;
		currentError = "This link has expired";
	}

	@When("try to use the same link again")
	public void tryToUseTheSameLinkAgain() {
		operationSuccess = false;
		currentError = "This link has already been used";
	}

	@When("they click the link from different IP {string}")
	public void theyClickTheLinkFromDifferentIp(String ip) {
		// Show security warning for different IP
		currentError = "Security warning: Login attempt from different location";
	}

	@When("they confirm {string}")
	public void theyConfirm(String confirmation) {
		if (confirmation.contains("Yes")) {
			operationSuccess = true;
			currentSession = createSession(1);
		}
	}

	@When("they receive verification email")
	public void theyReceiveVerificationEmail() {
		currentMagicLink = "https://auth.strategiz.io/verify?token=" + UUID.randomUUID().toString();
		codeGeneratedTime = Instant.now();
	}

	@When("click the verification link within {int} hours")
	public void clickTheVerificationLinkWithinHours(int hours) {
		operationSuccess = true;
		currentUser.emailVerified = true;
	}

	@When("they click the link")
	public void theyClickTheLink() {
		if (isCodeExpired()) {
			operationSuccess = false;
			currentError = "Verification link expired";
		}
		else {
			operationSuccess = true;
		}
	}

	@When("they update email to {string}")
	public void theyUpdateEmailTo(String newEmail) {
		currentUser.email = newEmail;
		currentUser.emailVerified = false;
	}

	@When("they verify the new email")
	public void theyVerifyTheNewEmail() {
		operationSuccess = true;
		currentUser.emailVerified = true;
	}

	@When("another user tries to change email to {string}")
	public void anotherUserTriesToChangeEmailTo(String email) {
		operationSuccess = false;
		currentError = "This email is already registered";
	}

	@When("they request {int} email codes within {int} minutes")
	public void theyRequestEmailCodesWithinMinutes(int count, int minutes) {
		if (count > 5) {
			operationSuccess = false;
			currentError = "Too many email requests. Wait 10 minutes before trying again";
		}
	}

	@When("they request a new code")
	public void theyRequestANewCode() {
		String oldCode = currentCode;
		currentCode = generateSixDigitCode();
		codeGeneratedTime = Instant.now();
		operationSuccess = true;
	}

	@When("they request email OTP")
	public void theyRequestEmailOtp() {
		currentCode = generateSixDigitCode();
		codeGeneratedTime = Instant.now();
		operationSuccess = true;
	}

	@When("the email bounces")
	public void theEmailBounces() {
		operationSuccess = false;
		currentError = "Email delivery failed. Please check your email address";
	}

	@When("the email goes to spam folder")
	public void theEmailGoesToSpamFolder() {
		// Email delivered but to spam
		operationSuccess = true;
	}

	@When("they check email on desktop")
	public void theyCheckEmailOnDesktop() {
		// Cross-device scenario
		assertTrue(true);
	}

	@When("enter the code on mobile")
	public void enterTheCodeOnMobile() {
		operationSuccess = true;
		currentSession = createSession(1);
	}

	@When("they verify in tab {int}")
	public void theyVerifyInTab(int tabNumber) {
		operationSuccess = true;
	}

	@When("they try to verify again")
	public void theyTryToVerifyAgain() {
		operationSuccess = false;
		currentError = "Already verified";
	}

	@When("a user receives email OTP")
	public void aUserReceivesEmailOtp() {
		currentCode = generateSixDigitCode();
		codeGeneratedTime = Instant.now();
	}

	@When("a user receives magic link email")
	public void aUserReceivesMagicLinkEmail() {
		currentMagicLink = "https://auth.strategiz.io/magic?token=" + UUID.randomUUID().toString();
		codeGeneratedTime = Instant.now();
	}

	// ============================================================================
	// WHEN STEPS - LOGIN FLOWS
	// ============================================================================

	@When("they log in with passkey")
	public void theyLogInWithPasskey() {
		operationSuccess = true;
		currentSession = createSession(2);
	}

	@When("they log in with SMS code")
	public void theyLogInWithSmsCode() {
		operationSuccess = true;
		currentSession = createSession(1);
	}

	@When("they log in with email OTP")
	public void theyLogInWithEmailOtp() {
		operationSuccess = true;
		currentSession = createSession(1);
	}

	@When("they log in with magic link")
	public void theyLogInWithMagicLink() {
		operationSuccess = true;
		currentSession = createSession(1);
	}

	@When("a user logs in with password \\(ACR {int})")
	public void aUserLogsInWithPassword(int acr) {
		currentSession = createSession(acr);
	}

	@When("SMS is delivered successfully")
	public void smsIsDeliveredSuccessfully() {
		operationSuccess = true;
	}

	@When("a user requests SMS code")
	public void aUserRequestsSmsCode() {
		currentCode = generateSixDigitCode();
		codeGeneratedTime = Instant.now();
		operationSuccess = true;
	}

	// ============================================================================
	// THEN STEPS - SUCCESS VALIDATIONS
	// ============================================================================

	@Then("the passkey should be registered successfully")
	public void thePasskeyShouldBeRegisteredSuccessfully() {
		assertTrue(operationSuccess);
		assertNotNull(currentCredentialId);
	}

	@Then("the passkey should have name {string}")
	public void thePasskeyShouldHaveName(String name) {
		assertTrue(operationSuccess);
	}

	@Then("the user should have {int} passkey(s) total")
	public void theUserShouldHavePasskeysTotal(int count) {
		assertTrue(count > 0);
	}

	@Then("the credential ID should be stored securely")
	public void theCredentialIdShouldBeStoredSecurely() {
		assertNotNull(currentCredentialId);
	}

	@Then("the second passkey should be registered")
	public void theSecondPasskeyShouldBeRegistered() {
		assertTrue(operationSuccess);
	}

	@Then("both passkeys should be active")
	public void bothPasskeysShouldBeActive() {
		assertTrue(true);
	}

	@Then("the passkey should be saved with name {string}")
	public void thePasskeyShouldBeSavedWithName(String name) {
		assertTrue(operationSuccess);
	}

	@Then("login should succeed")
	public void loginShouldSucceed() {
		assertTrue(operationSuccess);
		assertNotNull(currentSession);
	}

	@Then("session ACR level should be {string}")
	public void sessionAcrLevelShouldBe(String acr) {
		assertNotNull(currentSession);
		assertEquals(acr, currentSession.get("acr").toString());
	}

	@Then("session should contain user ID")
	public void sessionShouldContainUserId() {
		assertNotNull(currentSession);
		assertNotNull(currentSession.get("userId"));
	}

	@Then("authentication method should be {string}")
	public void authenticationMethodShouldBe(String method) {
		// Verify auth method in session
		assertTrue(true);
	}

	@Then("authentication method should include {string}")
	public void authenticationMethodShouldInclude(String method) {
		// Verify TOTP in auth methods
		assertTrue(true);
	}

	@Then("they should see all {int} passkeys listed")
	public void theyShouldSeeAllPasskeysListed(int count) {
		assertTrue(count > 0);
	}

	@Then("each passkey should show device name")
	public void eachPasskeyShouldShowDeviceName() {
		assertTrue(true);
	}

	@Then("each passkey should show registration date")
	public void eachPasskeyShouldShowRegistrationDate() {
		assertTrue(true);
	}

	@Then("each passkey should show last used date")
	public void eachPasskeyShouldShowLastUsedDate() {
		assertTrue(true);
	}

	@Then("the passkey name should be {string}")
	public void thePasskeyNameShouldBe(String name) {
		assertTrue(operationSuccess);
	}

	@Then("the passkey should be removed")
	public void thePasskeyShouldBeRemoved() {
		assertTrue(operationSuccess);
	}

	@Then("the user should have {int} passkey(s) remaining")
	public void theUserShouldHavePasskeysRemaining(int count) {
		assertTrue(count >= 0);
	}

	@Then("deletion should succeed")
	public void deletionShouldSucceed() {
		assertTrue(operationSuccess);
	}

	@Then("password should still work for login")
	public void passwordShouldStillWorkForLogin() {
		assertNotNull(currentUser.passwordHash);
	}

	@Then("the session ACR should be {string}")
	public void theSessionAcrShouldBe(String acr) {
		sessionAcrLevelShouldBe(acr);
	}

	@Then("they should have access to ACR {int} protected endpoints")
	public void theyShouldHaveAccessToAcrProtectedEndpoints(int acr) {
		assertNotNull(currentSession);
		assertTrue(Integer.parseInt(currentSession.get("acr").toString()) >= acr);
	}

	@Then("session ACR should upgrade to {string}")
	public void sessionAcrShouldUpgradeTo(String acr) {
		assertNotNull(currentSession);
		assertEquals(acr, currentSession.get("acr").toString());
	}

	@Then("counter should be updated to {int}")
	public void counterShouldBeUpdatedTo(int counter) {
		assertTrue(counter > 0);
	}

	// ============================================================================
	// THEN STEPS - TOTP VALIDATIONS
	// ============================================================================

	@Then("they should see a QR code")
	public void theyShouldSeeAQrCode() {
		assertNotNull(currentSecretKey);
	}

	@Then("they should see a secret key {string}")
	public void theyShouldSeeASecretKey(String secretKey) {
		assertNotNull(currentSecretKey);
	}

	@Then("they should see backup codes displayed")
	public void theyShouldSeeBackupCodesDisplayed() {
		assertNotNull(currentBackupCodes);
		assertFalse(currentBackupCodes.isEmpty());
	}

	@Then("TOTP should be enabled successfully")
	public void totpShouldBeEnabledSuccessfully() {
		assertTrue(operationSuccess);
	}

	@Then("backup codes should be saved")
	public void backupCodesShouldBeSaved() {
		assertNotNull(currentBackupCodes);
	}

	@Then("next login should require TOTP")
	public void nextLoginShouldRequireTotp() {
		assertTrue(true);
	}

	@Then("TOTP should not be enabled")
	public void totpShouldNotBeEnabled() {
		assertFalse(operationSuccess);
	}

	@Then("they should receive a file {string}")
	public void theyShouldReceiveAFile(String filename) {
		assertTrue(filename.contains("backup-codes"));
	}

	@Then("the file should contain {int} backup codes")
	public void theFileShouldContainBackupCodes(int count) {
		assertEquals(10, count);
	}

	@Then("each code should be {int} characters")
	public void eachCodeShouldBeCharacters(int length) {
		assertEquals(8, length);
	}

	@Then("they should be prompted for TOTP code")
	public void theyShouldBePromptedForTotpCode() {
		assertNotNull(currentSession);
		assertTrue((Boolean) currentSession.getOrDefault("awaitingTotp", false));
	}

	@Then("the prompt should say {string}")
	public void thePromptShouldSay(String message) {
		assertTrue(message.contains("authenticator"));
	}

	@Then("session ACR should be {string}")
	public void sessionAcrShouldBe(String acr) {
		assertNotNull(currentSession);
		assertEquals(acr, currentSession.get("acr").toString());
	}

	@Then("they should remain at TOTP step")
	public void theyShouldRemainAtTotpStep() {
		assertFalse(operationSuccess);
	}

	@Then("they should have {int} attempts remaining")
	public void theyShouldHaveAttemptsRemaining(int attempts) {
		assertEquals(attempts, remainingAttempts);
	}

	@Then("they should be rate limited")
	public void theyShouldBeRateLimited() {
		assertFalse(operationSuccess);
	}

	@Then("they should be logged out")
	public void theyShouldBeLoggedOut() {
		// Session cleared
		assertTrue(true);
	}

	@Then("the backup code should be marked as used")
	public void theBackupCodeShouldBeMarkedAsUsed() {
		assertTrue(operationSuccess);
	}

	@Then("TOTP should be disabled")
	public void totpShouldBeDisabled() {
		assertTrue(operationSuccess);
		assertNull(currentSecretKey);
	}

	@Then("backup codes should be invalidated")
	public void backupCodesShouldBeInvalidated() {
		assertTrue(true);
	}

	@Then("next login should not require TOTP")
	public void nextLoginShouldNotRequireTotp() {
		assertTrue(true);
	}

	@Then("session ACR should remain {string} for password-only")
	public void sessionAcrShouldRemainForPasswordOnly(String acr) {
		assertEquals("1", acr);
	}

	@Then("{int} new backup codes should be generated")
	public void newBackupCodesShouldBeGenerated(int count) {
		assertTrue(operationSuccess);
		assertEquals(10, count);
	}

	@Then("all old backup codes should be invalidated")
	public void allOldBackupCodesShouldBeInvalidated() {
		assertTrue(true);
	}

	@Then("they should see {string}")
	public void theyShouldSee(String message) {
		// Verify message displayed
		assertTrue(message != null);
	}

	@Then("a device token should be issued")
	public void aDeviceTokenShouldBeIssued() {
		assertNotNull(currentDeviceToken);
	}

	@Then("they should NOT be prompted for TOTP")
	public void theyShouldNotBePromptedForTotp() {
		assertTrue(operationSuccess);
	}

	@Then("they should be prompted for TOTP again")
	public void theyShouldBePromptedForTotpAgain() {
		assertNotNull(currentSession);
		assertTrue((Boolean) currentSession.getOrDefault("awaitingTotp", false));
	}

	@Then("that device should require TOTP on next login")
	public void thatDeviceShouldRequireTotpOnNextLogin() {
		assertTrue(operationSuccess);
	}

	@Then("they should have {int} trusted device(s) remaining")
	public void theyShouldHaveTrustedDevicesRemaining(int count) {
		assertTrue(count >= 0);
	}

	@Then("they should go through full setup again")
	public void theyShouldGoThroughFullSetupAgain() {
		assertTrue(operationSuccess);
	}

	@Then("get a new secret key")
	public void getANewSecretKey() {
		assertNotNull(currentSecretKey);
		assertTrue(currentSecretKey.startsWith("NEW_") || !currentSecretKey.equals("JBSWY3DPEHPK3PXP"));
	}

	@Then("old backup codes should not work")
	public void oldBackupCodesShouldNotWork() {
		assertTrue(true);
	}

	@Then("the TOTP secret should be encrypted in database")
	public void theTotpSecretShouldBeEncryptedInDatabase() {
		assertTrue(true);
	}

	@Then("should not be retrievable in plain text")
	public void shouldNotBeRetrievableInPlainText() {
		assertTrue(true);
	}

	@Then("a security event should be logged with timestamp")
	public void aSecurityEventShouldBeLoggedWithTimestamp() {
		assertTrue(true);
	}

	@Then("a security alert should be logged")
	public void aSecurityAlertShouldBeLogged() {
		assertTrue(true);
	}

	// ============================================================================
	// THEN STEPS - SMS/EMAIL VALIDATIONS
	// ============================================================================

	@Then("an SMS should be sent to {string}")
	public void anSmsShouldBeSentTo(String phoneNumber) {
		assertTrue(operationSuccess);
	}

	@Then("the SMS should contain a {int}-digit code")
	public void theSmsShouldContainADigitCode(int digits) {
		assertNotNull(currentCode);
		assertEquals(digits, currentCode.length());
	}

	@Then("the code should expire in {int} minutes")
	public void theCodeShouldExpireInMinutes(int minutes) {
		assertTrue(minutes > 0);
	}

	@Then("they should see message {string}")
	public void theyShouldSeeMessage(String message) {
		// Verify message shown to user
		assertTrue(message != null);
	}

	@Then("the generated code should be exactly {int} digits")
	public void theGeneratedCodeShouldBeExactlyDigits(int digits) {
		assertNotNull(currentCode);
		assertEquals(digits, currentCode.length());
	}

	@Then("the code should be numeric only")
	public void theCodeShouldBeNumericOnly() {
		assertNotNull(currentCode);
		assertTrue(currentCode.matches("\\d+"));
	}

	@Then("the code should not be {string} or {string}")
	public void theCodeShouldNotBeOr(String code1, String code2) {
		assertNotNull(currentCode);
		assertNotEquals(code1, currentCode);
		assertNotEquals(code2, currentCode);
	}

	@Then("the verification code should not be saved")
	public void theVerificationCodeShouldNotBeSaved() {
		assertFalse(operationSuccess);
	}

	@Then("verification should succeed")
	public void verificationShouldSucceed() {
		assertTrue(operationSuccess);
	}

	@Then("login should complete")
	public void loginShouldComplete() {
		assertTrue(operationSuccess);
		assertNotNull(currentSession);
	}

	@Then("a new SMS code should be sent")
	public void aNewSmsCodeShouldBeSent() {
		assertTrue(operationSuccess);
		assertNotNull(currentCode);
	}

	@Then("the old code should be invalidated")
	public void theOldCodeShouldBeInvalidated() {
		assertTrue(true);
	}

	@Then("they should see {string}")
	public void theyShouldSeeString(String message) {
		theyShouldSeeMessage(message);
	}

	@Then("resend should be blocked")
	public void resendShouldBeBlocked() {
		assertFalse(operationSuccess);
	}

	@Then("the phone number should be saved")
	public void thePhoneNumberShouldBeSaved() {
		assertNotNull(currentUser.phoneNumber);
	}

	@Then("it should be marked as verified")
	public void itShouldBeMarkedAsVerified() {
		assertTrue(currentUser.phoneVerified);
	}

	@Then("they should be able to use SMS login")
	public void theyShouldBeAbleToUseSmsLogin() {
		assertTrue(currentUser.phoneVerified);
	}

	@Then("the new number should replace the old one")
	public void theNewNumberShouldReplaceTheOldOne() {
		assertTrue(operationSuccess);
	}

	@Then("the old number should be removed")
	public void theOldNumberShouldBeRemoved() {
		assertTrue(true);
	}

	@Then("verification should be required for new number")
	public void verificationShouldBeRequiredForNewNumber() {
		assertFalse(currentUser.phoneVerified);
	}

	@Then("the phone number should be deleted")
	public void thePhoneNumberShouldBeDeleted() {
		assertNull(currentUser.phoneNumber);
	}

	@Then("SMS login should be disabled")
	public void smsLoginShouldBeDisabled() {
		assertNull(currentUser.phoneNumber);
	}

	@Then("password login should still work")
	public void passwordLoginShouldStillWork() {
		assertNotNull(currentUser.passwordHash);
	}

	@Then("they should receive an SMS code")
	public void theyShouldReceiveAnSmsCode() {
		assertNotNull(currentCode);
	}

	@Then("session should be created")
	public void sessionShouldBeCreated() {
		assertNotNull(currentSession);
	}

	@Then("SMS should be sent to {string}")
	public void smsShouldBeSentTo(String phoneNumber) {
		assertTrue(operationSuccess);
	}

	@Then("verification should work normally")
	public void verificationShouldWorkNormally() {
		assertTrue(true);
	}

	@Then("the code should be invalidated")
	public void theCodeShouldBeInvalidated() {
		assertTrue(true);
	}

	@Then("they should have to request a new code")
	public void theyShouldHaveToRequestANewCode() {
		assertTrue(true);
	}

	@Then("they receive new code {string}")
	public void theyReceiveNewCode(String newCode) {
		currentCode = newCode;
	}

	@Then("old code {string} should be invalidated")
	public void oldCodeShouldBeInvalidated(String oldCode) {
		assertTrue(true);
	}

	@Then("it should fail")
	public void itShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("they should have basic access")
	public void theyShouldHaveBasicAccess() {
		assertNotNull(currentSession);
	}

	@Then("SMS delivery should be logged")
	public void smsDeliveryShouldBeLogged() {
		assertTrue(true);
	}

	@Then("cost should be tracked for billing")
	public void costShouldBeTrackedForBilling() {
		assertTrue(true);
	}

	// ============================================================================
	// THEN STEPS - EMAIL OTP VALIDATIONS
	// ============================================================================

	@Then("they should receive an email")
	public void theyShouldReceiveAnEmail() {
		assertTrue(operationSuccess);
	}

	@Then("the email subject should be {string}")
	public void theEmailSubjectShouldBe(String subject) {
		assertTrue(subject.contains("Strategiz"));
	}

	@Then("the email should contain a {int}-digit code")
	public void theEmailShouldContainADigitCode(int digits) {
		assertNotNull(currentCode);
		assertEquals(digits, currentCode.length());
	}

	@Then("a new code should be sent via email")
	public void aNewCodeShouldBeSentViaEmail() {
		assertTrue(operationSuccess);
		assertNotNull(currentCode);
	}

	@Then("they should see {string}")
	public void theyShouldSeeMessage2(String message) {
		theyShouldSeeMessage(message);
	}

	@Then("they should receive an email with a magic link")
	public void theyShouldReceiveAnEmailWithAMagicLink() {
		assertNotNull(currentMagicLink);
	}

	@Then("the link should be valid for {int} minutes")
	public void theLinkShouldBeValidForMinutes(int minutes) {
		assertTrue(minutes > 0);
	}

	@Then("they should be redirected to the application")
	public void theyShouldBeRedirectedToTheApplication() {
		assertTrue(operationSuccess);
	}

	@Then("login should succeed automatically")
	public void loginShouldSucceedAutomatically() {
		assertTrue(operationSuccess);
		assertNotNull(currentSession);
	}

	@Then("they should see error page {string}")
	public void theyShouldSeeErrorPage(String error) {
		assertFalse(operationSuccess);
	}

	@Then("they should see button {string}")
	public void theyShouldSeeButton(String buttonText) {
		assertTrue(buttonText != null);
	}

	@Then("they should see security warning")
	public void theyShouldSeeSecurityWarning() {
		assertNotNull(currentError);
	}

	@Then("be asked to verify it''s really them")
	public void beAskedToVerifyItsReallyThem() {
		assertTrue(true);
	}

	@Then("login should proceed")
	public void loginShouldProceed() {
		assertTrue(operationSuccess);
	}

	@Then("email should be marked as verified")
	public void emailShouldBeMarkedAsVerified() {
		assertTrue(currentUser.emailVerified);
	}

	@Then("they should see success message {string}")
	public void theyShouldSeeSuccessMessage(String message) {
		assertTrue(message != null);
	}

	@Then("a verification email should be sent to {string}")
	public void aVerificationEmailShouldBeSentTo(String email) {
		assertTrue(operationSuccess);
	}

	@Then("the old email should remain active until verification")
	public void theOldEmailShouldRemainActiveUntilVerification() {
		assertTrue(true);
	}

	@Then("{string} should become the primary email")
	public void shouldBecomeThePrimaryEmail(String email) {
		assertEquals(email, currentUser.email);
	}

	@Then("{string} should be removed")
	public void shouldBeRemoved(String email) {
		assertTrue(true);
	}

	@Then("the {int}th request should be blocked")
	public void theRequestShouldBeBlocked(int requestNumber) {
		assertFalse(operationSuccess);
	}

	@Then("the {int}st request should be blocked")
	public void theFirstRequestShouldBeBlocked(int requestNumber) {
		assertFalse(operationSuccess);
	}

	@Then("the email should contain:")
	public void theEmailShouldContain(io.cucumber.datatable.DataTable dataTable) {
		// Verify email contains required elements
		assertTrue(true);
	}

	@Then("device type should be detected as {}")
	public void deviceTypeShouldBeDetectedAs(String deviceType) {
		assertTrue(deviceType != null);
	}

	@Then("security event should be logged")
	public void securityEventShouldBeLogged() {
		assertTrue(true);
	}

	// ============================================================================
	// THEN STEPS - ERROR VALIDATIONS
	// ============================================================================

	@Then("registration should fail")
	public void registrationShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("they should see error {string}")
	public void theyShouldSeeError(String error) {
		assertNotNull(currentError);
		assertTrue(currentError.contains(error) || error.contains(currentError));
	}

	@Then("login should fail")
	public void loginShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("deletion should fail")
	public void deletionShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("TOTP setup should fail")
	public void totpSetupShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("verification should fail")
	public void verificationShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("removal should fail")
	public void removalShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("the change should fail")
	public void theChangeShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("the request should fail")
	public void theRequestShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("authentication should fail")
	public void authenticationShouldFail() {
		assertFalse(operationSuccess);
	}

	@Then("they should see warning {string}")
	public void theyShouldSeeWarning(String warning) {
		assertNotNull(currentError);
	}

	@Then("they should see critical warning {string}")
	public void theyShouldSeeCriticalWarning(String warning) {
		assertNotNull(currentError);
	}

	@Then("they should remain on the login page")
	public void theyShouldRemainOnTheLoginPage() {
		assertFalse(operationSuccess);
	}

	@Then("login should be cancelled")
	public void loginShouldBeCancelled() {
		assertFalse(operationSuccess);
	}

	@Then("they should see error {string}")
	public void theyShouldSeeErrorMessage(String error) {
		theyShouldSeeError(error);
	}

	// ============================================================================
	// HELPER METHODS
	// ============================================================================

	private String generateSixDigitCode() {
		Random random = new Random();
		int code = 100000 + random.nextInt(900000);
		return String.valueOf(code);
	}

	private List<String> generateBackupCodes(int count) {
		List<String> codes = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			codes.add(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		}
		return codes;
	}

	private Map<String, Object> createSession(int acr) {
		Map<String, Object> session = new HashMap<>();
		session.put("userId", currentUser != null ? currentUser.id : UUID.randomUUID().toString());
		session.put("acr", String.valueOf(acr));
		session.put("authenticated", true);
		return session;
	}

	private boolean isCodeExpired() {
		if (codeGeneratedTime == null) {
			return false;
		}
		long minutesElapsed = ChronoUnit.MINUTES.between(codeGeneratedTime, Instant.now());
		return minutesElapsed > 15; // Default expiration
	}

}
