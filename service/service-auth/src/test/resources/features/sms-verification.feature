Feature: SMS Verification
  As a user without a password
  I want to verify my identity via SMS code
  So that I can log in securely using my phone number

  Background:
    Given the authentication service is available
    And SMS service is configured

  # SMS Code Request Scenarios
  Scenario: Request SMS verification code
    Given a user with phone number "+1-555-123-4567"
    When they request SMS verification
    Then an SMS should be sent to "+1-555-123-4567"
    And the SMS should contain a 6-digit code
    And the code should expire in 10 minutes
    And they should see message "Code sent to ***-***-4567"

  Scenario: SMS code format validation
    Given a user requests SMS verification
    Then the generated code should be exactly 6 digits
    And the code should be numeric only
    And the code should not be "000000" or "123456"

  Scenario: Rate limiting SMS requests
    Given a user with phone number "+1-555-123-4567"
    When they request 3 SMS codes within 1 minute
    Then the 4th request should be blocked
    And they should see error "Too many SMS requests. Wait 5 minutes before trying again"

  Scenario: SMS delivery failure handling
    Given a user requests SMS verification
    And SMS delivery fails (invalid phone number)
    Then they should see error "Unable to send SMS. Please check your phone number"
    And the verification code should not be saved

  # SMS Code Verification Scenarios
  Scenario: Verify SMS code successfully
    Given a user received SMS code "123456"
    And the code was sent 2 minutes ago
    When they enter code "123456"
    Then verification should succeed
    And session ACR should be "1"
    And authentication method should be "SMS"

  Scenario: Verify fails with wrong code
    Given a user received SMS code "123456"
    When they enter wrong code "654321"
    Then verification should fail
    And they should see error "Invalid verification code"
    And they should have 4 attempts remaining

  Scenario: SMS code expires after 10 minutes
    Given a user received SMS code "123456" 11 minutes ago
    When they enter the code "123456"
    Then verification should fail
    And they should see error "Verification code expired. Request a new one"

  Scenario: Resend SMS code
    Given a user received an SMS code
    When they click "Resend code"
    And wait 60 seconds
    Then a new SMS code should be sent
    And the old code should be invalidated
    And they should see "New code sent"

  Scenario: Cannot resend code within cooldown period
    Given a user just received an SMS code
    When they try to resend immediately
    Then resend should be blocked
    And they should see "Wait 60 seconds before requesting a new code"

  # Phone Number Management Scenarios
  Scenario: Add phone number to account
    Given a user with email "alice@example.com"
    And the user has no phone number
    When they add phone number "+1-555-999-8888"
    And verify it with SMS code "789012"
    Then the phone number should be saved
    And it should be marked as verified
    And they should be able to use SMS login

  Scenario: Change phone number
    Given a user has phone number "+1-555-111-2222" verified
    When they update to "+1-555-333-4444"
    And verify the new number with SMS code "456789"
    Then the new number should replace the old one
    And the old number should be removed
    And verification should be required for new number

  Scenario: Remove phone number
    Given a user has phone number "+1-555-123-4567"
    And has password authentication enabled
    When they remove the phone number
    Then the phone number should be deleted
    And SMS login should be disabled
    And password login should still work

  Scenario: Cannot remove phone if only auth method
    Given a user has only SMS authentication
    And no password or passkey
    When they try to remove phone number
    Then removal should fail
    And they should see error "Cannot remove phone number. It's your only login method"

  # SMS Login Flow Scenarios
  Scenario: Login with phone number
    Given a user with verified phone "+1-555-123-4567"
    When they choose "Sign in with phone number"
    And enter phone number "+1-555-123-4567"
    Then they should receive an SMS code
    When they enter the correct code
    Then login should succeed
    And session should be created

  Scenario: Login fails with unverified phone
    Given a user has phone "+1-555-999-8888" not verified
    When they try to log in with that number
    Then login should fail
    And they should see error "Phone number not verified. Please verify it first"

  # International Phone Number Scenarios
  Scenario Outline: Support international phone formats
    Given a user with phone number "<phone_number>"
    When they request SMS verification
    Then SMS should be sent to "<phone_number>"
    And verification should work normally

    Examples:
      | phone_number     |
      | +1-555-123-4567 | # US
      | +44-20-7946-0958| # UK
      | +91-98765-43210 | # India
      | +86-138-0013-8000| # China
      | +49-30-12345678 | # Germany

  Scenario: Invalid phone number format
    Given a user enters phone "123-4567"
    When they request SMS verification
    Then the request should fail
    And they should see error "Please enter a valid phone number with country code"

  # Security Scenarios
  Scenario: Maximum verification attempts
    Given a user received SMS code "123456"
    When they enter wrong code 5 times
    Then the code should be invalidated
    And they should see error "Too many failed attempts. Request a new code"
    And they should have to request a new code

  Scenario: One code active at a time
    Given a user received SMS code "111111"
    When they request a new code
    Then they receive new code "222222"
    And old code "111111" should be invalidated
    When they try to use old code "111111"
    Then it should fail

  Scenario: SMS codes are single-use
    Given a user received SMS code "123456"
    When they successfully verify with code "123456"
    And try to use the same code "123456" again
    Then it should fail
    And they should see error "This code has already been used"

  # Edge Cases
  Scenario: Verify code with leading/trailing spaces
    Given a user received SMS code "123456"
    When they enter " 123456 "
    Then verification should succeed
    # Code should be trimmed automatically

  Scenario Outline: Invalid code formats
    Given a user is at SMS verification step
    When they enter "<invalid_code>"
    Then they should see error "<error_message>"

    Examples:
      | invalid_code | error_message                    |
      | 12345       | Code must be 6 digits            |
      | 1234567     | Code must be 6 digits            |
      | ABCDEF      | Code must be numeric             |
      |             | Verification code is required    |

  # ACR Level Scenarios
  Scenario: SMS provides ACR level 1
    Given a user logs in with SMS code
    Then session ACR should be "1"
    And they should have basic access

  # Cost Management Scenarios
  Scenario: SMS delivery tracking
    Given a user requests SMS verification
    When SMS is delivered successfully
    Then SMS delivery should be logged
    And cost should be tracked for billing

  Scenario: Fallback when SMS service is down
    Given SMS service is unavailable
    When a user requests SMS code
    Then they should see error "SMS service temporarily unavailable. Try email verification instead"
