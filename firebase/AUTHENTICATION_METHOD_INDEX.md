# Authentication Method Firestore Index

## Overview
This document describes the Firestore composite index required for passkey (WebAuthn) authentication lookups.

## Required Index

### Collection Group: `security`
**Query Scope:** `COLLECTION_GROUP`

**Fields:**
1. `authentication_method` (ASCENDING)
2. `metadata.credentialId` (ASCENDING)
3. `isActive` (ASCENDING)

### Purpose
This index enables efficient collection group queries to find passkey credentials across all users' security subcollections during authentication.

### Query Pattern
```java
Query query = firestore.collectionGroup("security")
    .whereEqualTo("authentication_method", "PASSKEY")
    .whereEqualTo("isActive", true)
    .limit(100);
```

## Index Configuration

The index is defined in `firestore.indexes.json`:

```json
{
  "collectionGroup": "security",
  "queryScope": "COLLECTION_GROUP",
  "fields": [
    {
      "fieldPath": "authentication_method",
      "order": "ASCENDING"
    },
    {
      "fieldPath": "metadata.credentialId",
      "order": "ASCENDING"
    },
    {
      "fieldPath": "isActive",
      "order": "ASCENDING"
    }
  ]
}
```

## Deployment

### Deploy to Firebase
```bash
firebase deploy --only firestore:indexes
```

### Verify Deployment
1. Go to Firebase Console
2. Navigate to Firestore Database → Indexes
3. Verify the `security` collection group index exists with all three fields

## Field Name Standardization

**IMPORTANT:** The field name is `authentication_method` (with underscore) in Firestore.

### Java Entity Mapping
- **Firestore field:** `authentication_method`
- **Java field:** `authenticationMethod`
- **Annotation:** `@PropertyName("authentication_method")`

### Getter/Setter Names
- **Primary:** `getAuthenticationMethod()` / `setAuthenticationMethod()`
- **Backward compatible aliases:**
  - `getType()` / `setType()`
  - `getAuthenticationType()` / `setAuthenticationType()`

## Troubleshooting

### "Index Required" Error
If you see errors like:
```
The query requires an index. You can create it here: https://console.firebase.google.com/...
```

**Solution:** Deploy the indexes using `firebase deploy --only firestore:indexes`

### Credential Not Found
If passkey authentication fails with "Credential not found":

1. **Check index exists:** Verify in Firebase Console
2. **Check field name:** Ensure Firestore documents use `authentication_method` (not `type` or `authenticationType`)
3. **Check query:** Verify collection group query uses correct field name
4. **Check data:** Ensure passkey documents have `isActive=true` and correct `authentication_method="PASSKEY"`

## Related Files
- `firestore.indexes.json` - Index definitions
- `AuthenticationMethodEntity.java` - Entity with field mapping
- `AuthenticationMethodRepositoryImpl.java` - Repository with collection group query
- `PasskeyAuthenticationService.java` - Service using the query

## Last Updated
2025-10-12 - Field name standardization and graceful error handling
