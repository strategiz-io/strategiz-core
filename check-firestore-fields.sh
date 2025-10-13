#!/bin/bash

# Check what fields actually exist in a passkey document
echo "Checking Firestore document structure for user: 5e8a4f60-f2bd-494b-994c-08098e0c12eb"
echo ""
echo "Looking for documents in: users/5e8a4f60-f2bd-494b-994c-08098e0c12eb/security"
echo ""

# Use Firebase CLI to read the document
firebase firestore:get users/5e8a4f60-f2bd-494b-994c-08098e0c12eb/security --project strategiz-trading 2>&1 || echo "Failed to get document"
