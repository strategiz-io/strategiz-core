# Authentication BDD Tests

This directory contains Behavior-Driven Development (BDD) tests for the Strategiz authentication system, written in Gherkin syntax using Cucumber.

## Features Covered

### 1. Passkey Authentication (`passkey-authentication.feature`)
WebAuthn/FIDO2 passwordless authentication using biometric or hardware security keys.

**Scenarios:** 46 scenarios covering:
- Passkey registration (first device, backup devices, custom names)
- WebAuthn authentication and login
- Passkey management (list, rename, delete)
- ACR level 2 validation
- Security (counter validation, replay attack prevention)
- Edge cases (timeouts, cancellation, device types)

**Key Flows:**
- Register passkey → Login with passkey → ACR 2 session
- Multiple passkey management
- Cannot delete last passkey without password

### 2. TOTP Two-Factor Authentication (`totp-authentication.feature`)
Time-based One-Time Password (TOTP) for Google Authenticator and similar apps.

**Scenarios:** 40+ scenarios covering:
- TOTP setup (QR code, secret key, backup codes)
- Login with TOTP codes
- Backup code usage and regeneration
- Device trust (30-day remember)
- ACR level 2 validation
- Rate limiting after failed attempts

**Key Flows:**
- Enable TOTP → Scan QR → Verify code → TOTP active
- Login with password → Enter TOTP → ACR 2 session
- Lost device → Use backup code → Regenerate codes

### 3. SMS Verification (`sms-verification.feature`)
Phone-based authentication using SMS verification codes.

**Scenarios:** 30+ scenarios covering:
- SMS code request and delivery
- Code verification and expiration (10 minutes)
- Phone number management (add, change, remove)
- International phone number support
- Rate limiting (max 3 requests per minute)
- ACR level 1 validation

**Key Flows:**
- Enter phone → Receive SMS → Enter code → ACR 1 session
- Add phone to account → Verify with SMS
- Cannot remove phone if only auth method

### 4. Email OTP and Magic Links (`email-otp.feature`)
Email-based passwordless authentication using OTP codes and magic links.

**Scenarios:** 30+ scenarios covering:
- Email OTP code delivery and verification
- Magic link generation and validation
- Email verification for new accounts
- Link expiration (15 minutes) and single-use
- Cross-IP security warnings
- ACR level 1 validation

**Key Flows:**
- Request magic link → Click link → Auto-login → ACR 1 session
- Request email OTP → Enter code → Verify → ACR 1 session
- Change email → Verify new email → Old email removed

## Running the Tests

### Run All BDD Tests
```bash
mvn test -Dtest=CucumberTestRunner
```

### Run All Tests in Module
```bash
mvn test
```

### Run Specific Feature File
```bash
mvn test -Dcucumber.features=src/test/resources/features/passkey-authentication.feature
```

### Run Specific Scenario by Name
```bash
mvn test -Dcucumber.filter.name="Login with passkey successfully"
```

### Run Scenarios by Tag (future)
```bash
mvn test -Dcucumber.filter.tags="@acr-2"
```

## Test Architecture

### Directory Structure
```
service-auth/
├── src/test/java/io/strategiz/service/auth/bdd/
│   ├── AuthenticationSteps.java          # Step definitions (Given/When/Then)
│   ├── AuthBDDTestConfiguration.java     # Mock service configuration
│   ├── CucumberSpringConfiguration.java  # Spring Boot + Cucumber integration
│   └── CucumberTestRunner.java           # JUnit 5 test runner
└── src/test/resources/features/
    ├── passkey-authentication.feature    # WebAuthn/Passkey scenarios
    ├── totp-authentication.feature       # TOTP/Google Authenticator scenarios
    ├── sms-verification.feature          # SMS code scenarios
    └── email-otp.feature                 # Email OTP and magic link scenarios
```

### How It Works

1. **Feature Files** (Gherkin): Human-readable test specifications
   - Given: Setup (user state, environment)
   - When: Action (user interaction, API call)
   - Then: Assertion (expected outcome)

2. **Step Definitions** (Java): Implementation of Gherkin steps
   - Maps each Given/When/Then to Java code
   - Uses mocked services (no real SMS/email delivery)
   - Validates authentication flows and state transitions

3. **Mock Configuration**: Test-only services
   - No external API calls (SMS, email, WebAuthn hardware)
   - No database (Firestore excluded)
   - No Vault (PasetoTokenValidator mocked)

4. **Test Runner**: JUnit 5 Platform Suite
   - Discovers all .feature files
   - Executes scenarios via Cucumber engine
   - Generates HTML report in target/cucumber-reports/

## ACR (Authentication Context Class Reference) Levels

Tests validate proper ACR levels for each authentication method:

- **ACR 1**: Password-only, SMS, Email OTP, Magic Link
- **ACR 2**: Passkey, Password + TOTP, Backup codes

Protected endpoints require minimum ACR levels:
- Sensitive operations (transfers, settings changes) → ACR 2
- Standard operations (view data, read-only) → ACR 1

## Example Scenarios

### Passkey Registration
```gherkin
Scenario: Register first passkey successfully
  Given a user with email "alice@example.com" exists
  And the user has no passkeys registered
  When they initiate passkey registration
  And complete WebAuthn ceremony with credential ID "cred_abc123"
  Then the passkey should be registered successfully
  And the passkey should have name "MacBook Pro Touch ID"
  And session ACR level should be "2"
```

### TOTP Login
```gherkin
Scenario: TOTP verification succeeds
  Given a user is at the TOTP verification step
  And the current valid code is "654321"
  When they enter "654321"
  Then login should succeed
  And session ACR should be "2"
  And authentication method should include "TOTP"
```

### SMS Verification
```gherkin
Scenario: SMS code expires after 10 minutes
  Given a user received SMS code "123456" 11 minutes ago
  When they enter the code "123456"
  Then verification should fail
  And they should see error "Verification code expired. Request a new one"
```

### Magic Link Login
```gherkin
Scenario: Login with magic link successfully
  Given a user received a magic link
  And the link was generated 5 minutes ago
  When they click the magic link
  Then they should be redirected to the application
  And login should succeed automatically
  And session ACR should be "1"
```

## Test Reports

After running tests, view the HTML report:
```bash
open target/cucumber-reports/authentication-tests.html
```

Report includes:
- Scenario pass/fail status
- Step execution details
- Screenshots (if implemented)
- Execution time per scenario

## Adding New Scenarios

1. **Add to Feature File**: Write Gherkin scenario
   ```gherkin
   Scenario: New authentication flow
     Given user is at login page
     When they perform some action
     Then expected outcome occurs
   ```

2. **Implement Step Definitions**: Add to AuthenticationSteps.java
   ```java
   @When("they perform some action")
   public void theyPerformSomeAction() {
       // Implementation
   }
   ```

3. **Run Tests**: Verify new scenario passes
   ```bash
   mvn test -Dcucumber.filter.name="New authentication flow"
   ```

## Troubleshooting

### Tests fail with "No qualifying bean"
- Ensure AuthBDDTestConfiguration excludes Firebase/Firestore
- Check that PasetoTokenValidator is mocked in CucumberSpringConfiguration

### "Undefined step" errors
- Add missing step definition to AuthenticationSteps.java
- Ensure step regex matches Gherkin exactly

### Spring context initialization fails
- Check test properties disable Vault: `strategiz.vault.enabled=false`
- Verify all mocked services are marked @Primary

## Future Enhancements

- [ ] Add @tags for grouping scenarios (e.g., @passkey, @totp, @smoke)
- [ ] Implement screenshot capture for failed scenarios
- [ ] Add performance benchmarks for authentication flows
- [ ] Create integration tests with real SMS/email services
- [ ] Add security testing scenarios (SQL injection, XSS attempts)
