# Provider Connection Debug Flow

## Current Issue
- User "test111" has connected Kraken provider
- Provider data exists in Firestore database
- Frontend shows "Connect →" instead of "Connected ✓"

## Flow Trace

### 1. Frontend (DashboardScreen.tsx)
```javascript
// Line 96: Makes request to backend
const response = await fetch('https://localhost:8443/v1/providers', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

// Line 109-111: Checks for connected status
const connectedProvidersData = data.providers?.filter((p: any) => 
  p.status === 'CONNECTED' || p.status === 'connected' || p.connected === true
) || [];
```

### 2. Backend (ReadProviderController.java) 
- Receives GET /v1/providers
- Extracts userId from session token
- Calls ReadProviderService.getProvidersList(userId)

### 3. Backend (ReadProviderService.java)
- Calls providerIntegrationRepository.findAllEnabledByUserId(userId)
- This eventually calls ProviderIntegrationBaseRepository.findAllByUserId(userId)

### 4. Backend (ProviderIntegrationBaseRepository.java)
```java
// Line 104-105: Queries Firestore
Query query = getUserScopedCollection(userId)
    .whereIn("status", Arrays.asList("connected", "CONNECTED"));
```

### 5. Issue Identified
The problem is that the session token contains a different userId than what's stored in the database:
- Session userId: Current logged-in user's UUID
- Database: Provider is stored under different user's UUID

## Key Files to Check
1. /v1/providers endpoint - Where is userId coming from?
2. Session validation - What userId is in the token?
3. Database - Which user has the provider integration?

## Solution
Need to ensure the provider integration is saved under the correct userId when connecting.