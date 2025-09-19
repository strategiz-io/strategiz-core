#!/bin/bash

# Toggle demo mode directly in Firebase
USER_ID="fff74730-4a58-45fe-be74-cf38a57dcb0b"

echo "Toggling demo mode off for user: $USER_ID"

# Update the user's profile.demoMode to false in Firebase
firebase firestore:update users/$USER_ID --data '{"profile.demoMode": false}'

echo "Demo mode disabled in Firebase. The change should take effect immediately."