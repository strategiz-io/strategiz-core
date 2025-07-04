# Token Examples & Field Reference

## Complete Token Breakdown

### **Sample High-Assurance Token**

#### **Raw PASETO Token:**
```
v4.public.eyJzdWIiOiJ1c3JfcHViXzd4OWsybThwNG42cSIsImp0aSI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LTQ0NjY1NTQ0MDAwMCIsImlzcyI6InN0cmF0ZWdpei5pbyIsImF1ZCI6InN0cmF0ZWdpeiIsImlhdCI6MTcwMzk4MDgwMCwiZXhwIjoxNzA0MDY3MjAwLCJhbXIiOlsxLDNdLCJhY3IiOiIyLjMiLCJhdXRoX3RpbWUiOjE3MDM5ODA4MDAsInNjb3BlIjoicmVhZDpwb3J0Zm9saW8gd3JpdGU6cG9ydGZvbGlvIGFkbWluOnBvcnRmb2xpbyByZWFkOnBvc2l0aW9ucyB3cml0ZTpwb3NpdGlvbnMgZGVsZXRlOnBvc2l0aW9ucyJ9.signature_here
```

#### **Decoded JSON Payload:**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q",           // Subject - Public User ID (who the token belongs to)
  "jti": "550e8400-e29b-41d4-a716-446655440000",  // JWT ID - Unique token identifier for revocation/audit
  "iss": "strategiz.io",                    // Issuer - Who created and signed this token  
  "aud": "strategiz",                       // Audience - Who this token is intended for
  "iat": 1703980800,                       // Issued At - When token was created (Unix timestamp)
  "exp": 1704067200,                       // Expires At - When token becomes invalid (Unix timestamp)
  "amr": [1, 3],                          // Authentication Methods - [password, passkeys] (numeric mapping)
  "acr": "2.3",                           // Authentication Context Class - "2" (fully authenticated) + "3" (high assurance)
  "auth_time": 1703980800,                // Authentication Time - When user actually authenticated
  "scope": "read:portfolio write:portfolio admin:portfolio read:positions write:positions delete:positions"  // Permissions - What operations user can perform
}
```

---

## **Field-by-Field Reference** 📋

### **🆔 Subject (`sub`)**
```json
"sub": "usr_pub_7x9k2m8p4n6q"
```
- **Purpose**: Identifies the user this token belongs to
- **Format**: `usr_pub_` prefix + 16-character alphanumeric ID
- **Type**: Public User ID (safe to expose, doesn't reveal internal data)
- **Usage**: Primary identifier for API operations
- **Security**: No enumeration attacks possible, hides database structure

### **🎫 JWT ID (`jti`)**
```json
"jti": "550e8400-e29b-41d4-a716-446655440000"
```
- **Purpose**: Unique identifier for this specific token instance
- **Format**: UUID v4 (completely random)
- **Uniqueness**: Globally unique across all tokens ever issued
- **Usage**: Token revocation, audit trails, preventing replay attacks
- **Lifecycle**: Remains unique until token expires

### **🏢 Issuer (`iss`)**
```json
"iss": "strategiz.io"
```
- **Purpose**: Identifies who created and cryptographically signed this token
- **Format**: Domain name or URI
- **Validation**: Services must verify this matches expected issuer
- **Trust**: Establishes token authenticity and origin
- **Standard**: Official OIDC/JWT claim

### **🎯 Audience (`aud`)**
```json
"aud": "strategiz"
```
- **Purpose**: Identifies who this token is intended for
- **Format**: Service or application identifier
- **Validation**: Services must check they are the intended audience
- **Security**: Prevents token misuse across different systems
- **Scope**: All Strategiz services accept "strategiz" as valid audience

### **⏰ Issued At (`iat`)**
```json
"iat": 1703980800
```
- **Purpose**: Timestamp when the token was created
- **Format**: Unix timestamp (seconds since January 1, 1970 UTC)
- **Human Date**: December 30, 2023 12:00:00 PM UTC
- **Validation**: Can reject tokens with future issue times
- **Audit**: Helps track token creation patterns and timing

### **⌛ Expires At (`exp`)**
```json
"exp": 1704067200
```
- **Purpose**: Timestamp when the token becomes invalid
- **Format**: Unix timestamp (seconds since January 1, 1970 UTC)
- **Human Date**: December 31, 2023 12:00:00 PM UTC
- **Lifetime**: 24 hours from issuance (86,400 seconds)
- **Security**: Automatic expiration limits exposure window if compromised

### **🔐 Authentication Methods (`amr`)**
```json
"amr": [1, 3]
```
- **Purpose**: Records which authentication methods were used in this session
- **Format**: Array of numeric method identifiers (obfuscated)
- **Decoded Values**: 
  - `1` = `password` - User entered correct password
  - `3` = `passkeys` - User completed FIDO2/WebAuthn authentication
- **Security**: Numbers hide actual method names from potential attackers
- **Audit**: Complete trail of how user proved their identity

**Authentication Method Mapping:**
```
1 = password
2 = sms_otp  
3 = passkeys
4 = totp
5 = email_otp
6 = backup_codes
```

### **🏆 Authentication Context Class (`acr`)**
```json
"acr": "2.3"
```
- **Format**: `{authentication_level}.{assurance_level}`
- **Left Side "2"**: **Authentication Level** - User completed ALL required authentication steps
- **Right Side "3"**: **Assurance Level** - High assurance (hardware cryptographic authentication)

#### **Authentication Levels:**
- **"0"** = No authentication (anonymous)
- **"1"** = Partial authentication (profile created, but hasn't completed all required auth steps)
- **"2"** = Fully authenticated (user completed everything their account requires)

#### **Assurance Levels:**
- **".1"** = Basic assurance (single-factor authentication like password only)
- **".2"** = Substantial assurance (multi-factor authentication like password + TOTP)
- **".3"** = High assurance (hardware cryptographic authentication like passkeys)

#### **ACR "2.3" Interpretation:**
- ✅ **Fully Authenticated**: User completed all required authentication steps for their account
- ✅ **High Assurance**: Authentication used hardware cryptographic methods (passkeys)
- ✅ **Maximum Access**: Granted highest level of permissions and capabilities
- ✅ **NIST Equivalent**: Meets NIST SP 800-63 AAL3 (Authenticator Assurance Level 3)

### **⏱️ Authentication Time (`auth_time`)**
```json
"auth_time": 1703980800
```
- **Purpose**: Records when the user actually completed authentication
- **Format**: Unix timestamp (seconds since January 1, 1970 UTC)
- **Human Date**: December 30, 2023 12:00:00 PM UTC
- **Usage**: Step-up authentication decisions, session freshness validation
- **Security**: Helps determine if re-authentication is needed for sensitive operations

### **🔑 Scope (`scope`)**
```json
"scope": "read:portfolio write:portfolio admin:portfolio read:positions write:positions delete:positions"
```
- **Purpose**: Defines what operations the user is authorized to perform
- **Format**: Space-separated list of `{operation}:{resource}` pairs
- **Calculation**: Dynamically determined based on ACR level (2.3 = high assurance)
- **Granularity**: Operation-level permissions for fine-grained access control

#### **Decoded Permissions:**
- `read:portfolio` - Can view portfolio data and settings
- `write:portfolio` - Can modify portfolio configuration  
- `admin:portfolio` - Can perform administrative operations on portfolio
- `read:positions` - Can view current trading positions
- `write:positions` - Can create and modify trading positions
- `delete:positions` - Can close/delete positions (requires high assurance)

---

## **Token Validation Process** ✅

### **Step 1: Structure Validation**
```javascript
// Token must have exactly 3 parts separated by dots
const parts = token.split('.');
if (parts.length !== 3) return false;

// First part must be "v4.public"
if (parts[0] !== 'v4.public') return false;
```

### **Step 2: Signature Verification**
```javascript
// Verify Ed25519 signature using public key
const header = parts[0] + '.' + parts[1] + '.';
const signature = base64UrlDecode(parts[2]);
const isValidSignature = ed25519.verify(publicKey, header, signature);
```

### **Step 3: Claims Validation**
```javascript
const claims = JSON.parse(base64UrlDecode(parts[1]));

// Check expiration
const isNotExpired = claims.exp > Math.floor(Date.now() / 1000);

// Check audience
const isCorrectAudience = claims.aud === 'strategiz';

// Check issuer  
const isCorrectIssuer = claims.iss === 'strategiz.io';

// Check if token is revoked
const isNotRevoked = !isTokenRevoked(claims.jti);
```

### **Step 4: Access Control**
```javascript
// Check if user has required assurance level
const hasRequiredAccess = meetsMinimumAcr(claims.acr, requiredAcr);

// Check if user has specific permission
const hasPermission = claims.scope.split(' ').includes(requiredScope);
```

---

## **Progressive Authentication Examples** 🔄

### **Profile Created (ACR "1")**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q",
  "acr": "1",                              // Partial authentication only
  "amr": [],                               // No authentication methods completed yet
  "scope": "read:profile write:profile read:auth_methods write:auth_methods"
}
```

### **Password Only (ACR "2.1")**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q", 
  "acr": "2.1",                            // Fully authenticated, basic assurance
  "amr": [1],                              // Password authentication only
  "scope": "read:profile write:profile read:portfolio read:positions read:market_data"
}
```

### **Multi-Factor (ACR "2.2")**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q",
  "acr": "2.2",                            // Fully authenticated, substantial assurance  
  "amr": [1, 4],                           // Password + TOTP
  "scope": "read:profile write:profile read:portfolio write:portfolio read:positions write:positions read:trades write:trades"
}
```

### **Passkeys (ACR "2.3")**
```json
{
  "sub": "usr_pub_7x9k2m8p4n6q",
  "acr": "2.3",                            // Fully authenticated, high assurance
  "amr": [3],                              // Passkeys only (sufficient for high assurance)
  "scope": "read:portfolio write:portfolio admin:portfolio read:positions write:positions delete:positions read:trades write:trades delete:trades admin:strategies admin:settings"
}
```

---

## **Security Considerations** 🛡️

### **Token Exposure**
- ✅ **No Sensitive Data**: Token contains no passwords, secrets, or PII
- ✅ **Public Claims**: All claims are safe to decode and inspect
- ✅ **Tamper-Proof**: Signature prevents any modifications
- ✅ **Time-Limited**: 24-hour expiration limits exposure window

### **Authentication Strength**
- ✅ **Progressive Security**: Higher ACR = more permissions
- ✅ **Method Tracking**: Complete audit trail via AMR
- ✅ **Hardware Crypto**: Passkeys provide highest assurance
- ✅ **Step-up Auth**: Can require higher ACR for sensitive operations

### **Access Control**
- ✅ **Least Privilege**: Minimal scopes granted based on authentication strength
- ✅ **Operation-Level**: Granular permissions (read vs write vs delete vs admin)
- ✅ **Resource Isolation**: Each service validates its own resource permissions
- ✅ **Dynamic Permissions**: Scopes calculated at token creation time

This token represents a **premium user with maximum authentication assurance** who can perform **all available operations** including sensitive administrative functions. 🚀 