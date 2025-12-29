Feature: Passkey Authentication (WebAuthn)
  As a security-conscious user
  I want to use passkeys for passwordless authentication
  So that I can log in quickly and securely without passwords

  Background:
    Given the authentication service is available
    And WebAuthn is supported

  # Registration Scenarios
  Scenario: Register first passkey successfully
    Given a user with email "alice@example.com" exists
    And the user has no passkeys registered
    When they initiate passkey registration
    And complete WebAuthn ceremony with credential ID "cred_abc123"
    Then the passkey should be registered successfully
    And the passkey should have name "MacBook Pro Touch ID"
    And the user should have 1 passkey total
    And the credential ID should be stored securely

  Scenario: Register second passkey (backup device)
    Given a user has 1 passkey registered
    When they register another passkey with credential ID "cred_xyz789"
    Then the second passkey should be registered
    And the user should have 2 passkeys total
    And both passkeys should be active

  Scenario: Cannot register duplicate credential
    Given a user has passkey with credential ID "cred_abc123"
    When they try to register the same credential ID again
    Then registration should fail
    And they should see error "This passkey is already registered"

  Scenario: Register passkey with custom name
    Given a user initiates passkey registration
    When they complete WebAuthn ceremony
    And provide custom name "iPhone 15 Face ID"
    Then the passkey should be saved with name "iPhone 15 Face ID"

  # Authentication Scenarios
  Scenario: Login with passkey successfully
    Given a user has passkey "cred_abc123" registered
    When they visit the login page
    And click "Sign in with Passkey"
    And complete WebAuthn authentication with credential "cred_abc123"
    Then login should succeed
    And session ACR level should be "2"
    And session should contain user ID
    And authentication method should be "PASSKEY"

  Scenario: Login fails with wrong credential
    Given a user has passkey "cred_abc123" registered
    When they attempt WebAuthn authentication with credential "cred_wrong"
    Then login should fail
    And they should see error "Invalid passkey"

  Scenario: Login with revoked passkey fails
    Given a user has passkey "cred_abc123" registered
    And the passkey was revoked
    When they attempt WebAuthn authentication with credential "cred_abc123"
    Then login should fail
    And they should see error "This passkey has been revoked"

  # Passkey Management Scenarios
  Scenario: List all registered passkeys
    Given a user has 3 passkeys registered
    When they view their security settings
    Then they should see all 3 passkeys listed
    And each passkey should show device name
    And each passkey should show registration date
    And each passkey should show last used date

  Scenario: Rename a passkey
    Given a user has passkey named "MacBook Pro"
    When they rename it to "Work Laptop"
    Then the passkey name should be "Work Laptop"

  Scenario: Delete a passkey
    Given a user has 2 passkeys registered
    When they delete passkey "cred_abc123"
    Then the passkey should be removed
    And the user should have 1 passkey remaining
    And they should see success message "Passkey removed"

  Scenario: Cannot delete last passkey if no password
    Given a user has only 1 passkey
    And the user has no password set
    When they try to delete the last passkey
    Then deletion should fail
    And they should see error "Cannot remove last authentication method. Set a password first."

  Scenario: Can delete last passkey if password exists
    Given a user has 1 passkey
    And the user has a password set
    When they delete the last passkey
    Then deletion should succeed
    And password should still work for login

  # ACR Level Scenarios
  Scenario: Passkey provides ACR level 2
    Given a user logs in with passkey
    Then the session ACR should be "2"
    And they should have access to ACR 2 protected endpoints

  Scenario: Upgrade from password to passkey increases ACR
    Given a user logged in with password (ACR 1)
    When they add and verify a passkey in the same session
    Then session ACR should upgrade to "2"

  # Edge Cases
  Scenario: WebAuthn ceremony timeout
    Given a user initiates passkey registration
    When the WebAuthn ceremony times out after 60 seconds
    Then registration should fail
    And they should see error "Passkey registration timed out. Please try again."

  Scenario: User cancels WebAuthn prompt
    Given a user initiates passkey login
    When they cancel the WebAuthn browser prompt
    Then login should be cancelled
    And they should remain on the login page

  Scenario Outline: Passkey registration with various device types
    Given a user on <device_type>
    When they register a passkey
    Then the passkey should be stored
    And device type should be detected as <device_type>

    Examples:
      | device_type           |
      | macOS Touch ID        |
      | Windows Hello         |
      | iPhone Face ID        |
      | Android Fingerprint   |
      | Hardware Security Key |

  # Security Scenarios
  Scenario: Counter validation prevents replay attacks
    Given a user has passkey with counter value 5
    When they authenticate and counter is 6
    Then authentication should succeed
    And counter should be updated to 6
    When they try to authenticate with counter 5 again
    Then authentication should fail
    And they should see error "Potential replay attack detected"

  Scenario: Credential public key is validated
    Given a user has passkey registered
    When they authenticate with invalid signature
    Then authentication should fail
    And security event should be logged
