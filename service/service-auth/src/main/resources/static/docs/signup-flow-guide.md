# Multi-Step Signup Flow Guide

This document explains how to implement a secure multi-step signup flow using Strategiz microservices.

## Overview

The signup flow consists of the following steps:
1. Email verification (optional but recommended)
2. Profile creation
3. Passkey registration
4. Product service onboarding (future)

## Client-Side Orchestration

This flow uses client-side orchestration, meaning the frontend is responsible for:
1. Managing the flow between steps
2. Storing temporary state during the signup process
3. Calling appropriate backend service endpoints in sequence

## Service Endpoints

### Step 1: Email Verification

**Send verification code:**
```
POST /auth/verification/email/send-code
{
  "email": "user@example.com",
  "purpose": "signup"
}
```

**Verify code:**
```
POST /auth/verification/email/verify-code
{
  "email": "user@example.com",
  "purpose": "signup",
  "code": "123456"
}
```

**Check verification status:**
```
GET /auth/verification/email/status?email=user@example.com&purpose=signup
```

### Step 2: Profile Creation

```
POST /api/profile
{
  "email": "user@example.com",
  "name": "User Name",
  ... other profile fields
}
```

### Step 3: Passkey Registration

**Begin registration:**
```
POST /auth/passkey/registration/begin
{
  "email": "user@example.com",
  "displayName": "User Name",
  "verificationCode": "123456"  // Optional but recommended
}
```

**Complete registration:**
```
POST /auth/passkey/registration/complete
{
  "email": "user@example.com",
  "credentialId": "...",
  "attestationResponse": "..."
}
```

## Implementation Flow

1. **Email Verification:**
   - Request a verification code to be sent to the user's email
   - Display a form for the user to enter the code
   - Verify the code with the backend

2. **Profile Creation:**
   - Collect profile information from the user
   - Submit to the profile service
   - Store the profile ID for later use

3. **Passkey Registration:**
   - Begin registration with the auth service, including the verification code
   - Use the WebAuthn API to create credentials on the user's device
   - Complete registration with the auth service

## Security Considerations

1. Email verification helps prevent spam accounts and verifies user identity
2. Passkeys provide strong phishing-resistant authentication
3. Service isolation ensures separation of concerns and better security

## Development vs. Production

In development mode, email verification can be optional to streamline testing. In production, it should be required.

## Example JavaScript Implementation

Below is a simplified example of how to implement this flow using JavaScript:

```javascript
// Step 1: Email Verification
async function startEmailVerification(email) {
  const response = await fetch('/auth/verification/email/send-code', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, purpose: 'signup' })
  });
  return response.json();
}

async function verifyEmailCode(email, code) {
  const response = await fetch('/auth/verification/email/verify-code', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, purpose: 'signup', code })
  });
  return response.json();
}

// Step 2: Profile Creation
async function createProfile(profileData) {
  const response = await fetch('/api/profile', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(profileData)
  });
  return response.json();
}

// Step 3: Passkey Registration
async function beginPasskeyRegistration(email, displayName, verificationCode) {
  const response = await fetch('/auth/passkey/registration/begin', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, displayName, verificationCode })
  });
  const data = await response.json();
  
  // Use WebAuthn API to create credentials
  const credential = await navigator.credentials.create({
    publicKey: data.publicKeyCredentialCreationOptions
  });
  
  return { credential, data };
}

async function completePasskeyRegistration(email, credential, challenge) {
  const response = await fetch('/auth/passkey/registration/complete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      credentialId: credential.id,
      attestationResponse: credential.response
    })
  });
  return response.json();
}
```
