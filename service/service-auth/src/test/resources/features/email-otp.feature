Feature: Email OTP and Magic Link Authentication
  As a user without a password
  I want to log in using a code or link sent to my email
  So that I can access my account without remembering a password

  Background:
    Given the authentication service is available
    And email service is configured

  # Email OTP Code Scenarios
  Scenario: Request email OTP code
    Given a user with email "alice@example.com"
    When they choose "Sign in with email"
    And enter email address "alice@example.com"
    Then they should receive an email
    And the email subject should be "Your Strategiz verification code"
    And the email should contain a 6-digit code
    And the code should expire in 15 minutes
    And they should see message "Code sent to ali***@example.com"

  Scenario: Verify email OTP successfully
    Given a user received email code "789012"
    And the code was sent 5 minutes ago
    When they enter code "789012"
    Then verification should succeed
    And login should complete
    And session ACR should be "1"
    And authentication method should be "EMAIL_OTP"

  Scenario: Email OTP code expires
    Given a user received email code "789012" 16 minutes ago
    When they enter the code "789012"
    Then verification should fail
    And they should see error "Verification code expired. Request a new one"

  Scenario: Wrong email OTP code
    Given a user received email code "789012"
    When they enter wrong code "123456"
    Then verification should fail
    And they should see error "Invalid verification code"
    And they should have 4 attempts remaining

  Scenario: Resend email OTP
    Given a user received an email code
    When they click "Resend code"
    Then a new code should be sent via email
    And the old code should be invalidated
    And they should see "New code sent to your email"

  # Magic Link Scenarios
  Scenario: Request magic link
    Given a user with email "bob@example.com"
    When they choose "Sign in with email"
    And enter email "bob@example.com"
    And select "Send me a magic link instead"
    Then they should receive an email with a magic link
    And the email subject should be "Your Strategiz sign-in link"
    And the link should be valid for 15 minutes

  Scenario: Login with magic link successfully
    Given a user received a magic link
    And the link was generated 5 minutes ago
    When they click the magic link
    Then they should be redirected to the application
    And login should succeed automatically
    And session ACR should be "1"
    And authentication method should be "MAGIC_LINK"

  Scenario: Magic link expires after 15 minutes
    Given a user received a magic link 16 minutes ago
    When they click the expired link
    Then they should see error page "This link has expired"
    And they should see button "Request a new link"

  Scenario: Magic link is single-use
    Given a user received a magic link
    When they click the link and log in successfully
    And try to use the same link again
    Then they should see error "This link has already been used"

  Scenario: Magic link from different IP is suspicious
    Given a user requested magic link from IP "192.168.1.100"
    When they click the link from different IP "203.0.113.50"
    Then they should see security warning
    And be asked to verify it's really them
    When they confirm "Yes, this was me"
    Then login should proceed

  # Email Verification for New Accounts
  Scenario: Verify email for new account
    Given a new user registered with email "charlie@example.com"
    And email is not yet verified
    When they receive verification email
    And click the verification link within 24 hours
    Then email should be marked as verified
    And they should see success message "Email verified!"

  Scenario: Email verification link expires
    Given a user received email verification link 25 hours ago
    When they click the link
    Then they should see error "Verification link expired"
    And they should see "Resend verification email" button

  # Email Management Scenarios
  Scenario: Change email address
    Given a user with email "old@example.com"
    When they update email to "new@example.com"
    Then a verification email should be sent to "new@example.com"
    And the old email should remain active until verification
    When they verify the new email
    Then "new@example.com" should become the primary email
    And "old@example.com" should be removed

  Scenario: Cannot change to email already in use
    Given user "alice@example.com" exists
    When another user tries to change email to "alice@example.com"
    Then the change should fail
    And they should see error "This email is already registered"

  # Rate Limiting Scenarios
  Scenario: Rate limit email OTP requests
    Given a user with email "test@example.com"
    When they request 5 email codes within 5 minutes
    Then the 6th request should be blocked
    And they should see error "Too many email requests. Wait 10 minutes before trying again"

  Scenario: Rate limit magic link requests
    Given a user requests 3 magic links within 2 minutes
    Then the 4th request should be blocked
    And they should see error "Too many requests. Please wait before requesting another link"

  # Security Scenarios
  Scenario: Email OTP codes are single-use
    Given a user received email code "123456"
    When they successfully verify with "123456"
    And try to use code "123456" again
    Then verification should fail
    And they should see error "This code has already been used"

  Scenario: One active code per email at a time
    Given a user received email code "111111"
    When they request a new code
    Then they receive new code "222222"
    And old code "111111" should be invalidated

  Scenario: Magic link token is cryptographically secure
    Given a user requests a magic link
    Then the link token should be at least 32 characters
    And should be randomly generated
    And should not be predictable

  # Edge Cases
  Scenario Outline: Invalid email OTP code formats
    Given a user is at email verification step
    When they enter "<invalid_code>"
    Then they should see error "<error_message>"

    Examples:
      | invalid_code | error_message                    |
      | 12345       | Code must be 6 digits            |
      | 1234567     | Code must be 6 digits            |
      | ABCDEF      | Code must be numeric             |
      |             | Verification code is required    |

  Scenario: Email bounces should be handled
    Given a user with email "invalid@nonexistent-domain.com"
    When they request email OTP
    And the email bounces
    Then they should see error "Email delivery failed. Please check your email address"

  Scenario: Spam filter handling
    Given a user requests email OTP
    And the email goes to spam folder
    Then they should see message "Check your spam folder if you don't see the email"

  # Multi-Device Scenarios
  Scenario: Request code on mobile, verify on desktop
    Given a user requests email OTP on mobile
    When they check email on desktop
    And enter the code on mobile
    Then verification should succeed
    # Code should work across devices

  Scenario: Multiple open verification sessions
    Given a user has email verification open in 2 browser tabs
    When they verify in tab 1
    Then tab 2 should show "Already verified" when they try to verify again

  # Email Template Scenarios
  Scenario: Email OTP template contains required elements
    Given a user receives email OTP
    Then the email should contain:
      | Element              |
      | 6-digit code         |
      | Expiration time      |
      | "If you didn't request this" warning |
      | Strategiz logo       |
      | Support contact      |

  Scenario: Magic link email template
    Given a user receives magic link email
    Then the email should contain:
      | Element              |
      | Clickable magic link |
      | Link expiration time |
      | Browser/device info  |
      | "If you didn't request this" warning |

  # ACR Level Scenarios
  Scenario: Email OTP provides ACR level 1
    Given a user logs in with email OTP
    Then session ACR should be "1"
    And they should have basic access

  Scenario: Magic link provides ACR level 1
    Given a user logs in with magic link
    Then session ACR should be "1"
    And authentication method should be "MAGIC_LINK"
