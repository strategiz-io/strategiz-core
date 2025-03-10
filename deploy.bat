@echo off
echo ===== Strategiz Core API Deployment =====
echo.
echo Building TypeScript...
call npm run build

echo.
echo Deploying to Firebase...
call firebase deploy --only functions

echo.
echo Deployment complete!
echo Your API is now available at: https://us-central1-strategiz-io.cloudfunctions.net/api
echo.
pause
