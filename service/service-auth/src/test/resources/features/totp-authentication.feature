Feature: TOTP Two-Factor Authentication
  As a security-conscious user
  I want to enable TOTP (Google Authenticator) for two-factor authentication
  So that my account is protected with a second factor

  Background:
    Given the authentication service is available
    And the user is authenticated

  # TOTP Setup Scenarios
  Scenario: Enable TOTP for the first time
    Given a user with email "bob@example.com"
    And the user has no TOTP enabled
    When they navigate to security settings
    And click "Enable Two-Factor Authentication"
    Then they should see a QR code
    And they should see a secret key "JBSWY3DPEHPK3PXP"
    And they should see backup codes displayed

  Scenario: Confirm TOTP setup with valid code
    Given a user is setting up TOTP
    And the secret key is "JBSWY3DPEHPK3PXP"
    When they scan the QR code with Google Authenticator
    And enter valid TOTP code "123456"
    Then TOTP should be enabled successfully
    And they should see success message "Two-factor authentication enabled"
    And backup codes should be saved
    And next login should require TOTP

  Scenario: Cannot confirm TOTP with invalid code
    Given a user is setting up TOTP
    When they enter invalid code "000000"
    Then TOTP setup should fail
    And they should see error "Invalid verification code"
    And TOTP should not be enabled

  Scenario: Download backup codes during setup
    Given a user successfully enabled TOTP
    When they download backup codes
    Then they should receive a file "strategiz-backup-codes.txt"
    And the file should contain 10 backup codes
    And each code should be 8 characters

  # TOTP Login Scenarios
  Scenario: Login with TOTP enabled
    Given a user has TOTP enabled
    When they log in with correct email and password
    Then they should be prompted for TOTP code
    And the prompt should say "Enter code from your authenticator app"

  Scenario: TOTP verification succeeds
    Given a user is at the TOTP verification step
    And the current valid code is "654321"
    When they enter "654321"
    Then login should succeed
    And session ACR should be "2"
    And authentication method should include "TOTP"

  Scenario: TOTP verification fails with wrong code
    Given a user is at the TOTP verification step
    When they enter incorrect code "111111"
    Then login should fail
    And they should see error "Invalid verification code"
    And they should remain at TOTP step
    And they should have 4 attempts remaining

  Scenario: TOTP code expires after 30 seconds
    Given a user is at the TOTP verification step
    And a code "123456" was valid 35 seconds ago
    When they enter the expired code "123456"
    Then they should see error "Code expired. Enter the current code from your app"

  Scenario: Rate limiting after multiple failed attempts
    Given a user is at the TOTP verification step
    When they enter wrong code 5 times
    Then they should be rate limited
    And they should see error "Too many failed attempts. Try again in 15 minutes"
    And they should be logged out

  # Backup Code Scenarios
  Scenario: Login with backup code
    Given a user has TOTP enabled
    And they completed password authentication
    When they click "Use backup code instead"
    And enter backup code "ABCD1234"
    Then login should succeed
    And the backup code should be marked as used
    And they should see warning "This backup code has been used"

  Scenario: Cannot reuse backup code
    Given a user has backup code "ABCD1234" already used
    When they try to log in with "ABCD1234"
    Then login should fail
    And they should see error "This backup code has already been used"

  Scenario: All backup codes used warning
    Given a user used 9 out of 10 backup codes
    When they log in with the last backup code
    Then login should succeed
    And they should see critical warning "All backup codes used! Generate new ones immediately"

  # TOTP Management Scenarios
  Scenario: Disable TOTP
    Given a user has TOTP enabled
    When they navigate to security settings
    And click "Disable Two-Factor Authentication"
    And confirm with their password
    Then TOTP should be disabled
    And backup codes should be invalidated
    And next login should not require TOTP
    And session ACR should remain "1" for password-only

  Scenario: Regenerate backup codes
    Given a user has TOTP enabled
    And has 10 backup codes
    When they click "Generate new backup codes"
    And confirm the action
    Then 10 new backup codes should be generated
    And all old backup codes should be invalidated
    And they should see success message "New backup codes generated. Download them now."

  Scenario: View remaining backup codes
    Given a user has TOTP enabled
    And used 3 backup codes
    When they view security settings
    Then they should see "7 backup codes remaining"

  # Device Trust Scenarios
  Scenario: Remember device for 30 days
    Given a user has TOTP enabled
    When they log in and check "Trust this device for 30 days"
    And enter valid TOTP code
    Then login should succeed
    And a device token should be issued
    When they log in again from the same device within 30 days
    Then they should NOT be prompted for TOTP

  Scenario: Device trust expires after 30 days
    Given a user trusted a device 31 days ago
    When they log in from that device
    Then they should be prompted for TOTP again

  Scenario: Revoke trusted device
    Given a user has 2 trusted devices
    When they navigate to security settings
    And revoke trust for "Chrome on Windows"
    Then that device should require TOTP on next login
    And they should have 1 trusted device remaining

  # ACR Level Scenarios
  Scenario: TOTP provides ACR level 2
    Given a user logs in with password + TOTP
    Then session ACR should be "2"
    And they should have access to ACR 2 protected endpoints

  Scenario: Password-only login has ACR 1
    Given a user has TOTP enabled
    But bypasses it with backup code
    Then session ACR should be "2"
    # Note: backup code still counts as 2FA

  # Edge Cases
  Scenario Outline: Invalid TOTP code formats
    Given a user is at TOTP verification step
    When they enter "<invalid_code>"
    Then they should see error "<error_message>"

    Examples:
      | invalid_code | error_message                 |
      | 12345       | Code must be 6 digits         |
      | 1234567     | Code must be 6 digits         |
      | ABCDEF      | Code must be numeric          |
      |             | TOTP code is required         |
      | 000000      | Invalid verification code     |

  Scenario: Clock skew tolerance
    Given a user has TOTP enabled
    And server clock is 25 seconds ahead
    When they enter code that's valid for current time
    Then login should succeed
    # TOTP should tolerate ±30 seconds clock drift

  Scenario: Re-enable TOTP after disabling
    Given a user previously had TOTP enabled
    And they disabled it
    When they enable TOTP again
    Then they should go through full setup again
    And get a new secret key
    And old backup codes should not work

  # Security Scenarios
  Scenario: TOTP secret is encrypted at rest
    Given a user enables TOTP
    Then the TOTP secret should be encrypted in database
    And should not be retrievable in plain text

  Scenario: Security event logging
    Given a user has TOTP enabled
    When they log in with TOTP
    Then a security event should be logged with timestamp
    When they fail TOTP 3 times
    Then a security alert should be logged
