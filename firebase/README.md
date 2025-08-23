# Firebase Configuration

This directory contains Firebase Firestore configuration for the Strategiz backend.

## Files

- `firestore.rules` - Security rules for Firestore database
- `firestore.indexes.json` - Composite indexes required for complex queries
- `firebase.json` - Firebase configuration file
- `.firebaserc` - Firebase project configuration

## Deployment

To deploy Firestore rules and indexes:

```bash
cd firebase
firebase deploy --only firestore
```

To deploy only indexes:
```bash
firebase deploy --only firestore:indexes
```

To deploy only rules:
```bash
firebase deploy --only firestore:rules
```

## Important Notes

- The frontend repository (`strategiz-ui`) handles Firebase Hosting deployment
- This backend repository manages Firestore database configuration
- Both repositories share the same Firebase project (`strategiz-io`)

## Current Indexes

### Security Collection Group
- Used for passkey authentication across all users
- Fields: `authentication_method`, `metadata.credentialId`, `isActive`

## Security Rules

Currently includes:
- User-specific data access control
- Security subcollection management
- Passkey challenges for authentication
- Session management

⚠️ **Note**: Remove the catch-all rule (`allow read, write: if true`) in production!