@echo off
echo ===== Strategiz Core API Deployment =====
echo.

echo Creating Firebase hosting target (if not exists)...
call firebase target:apply hosting api api-strategiz-io
if %ERRORLEVEL% NEQ 0 (
    echo Firebase target creation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo Building Spring Boot application with Maven...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo Maven build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo Building Docker image...
call gcloud builds submit --tag gcr.io/strategiz-io/strategiz-core
if %ERRORLEVEL% NEQ 0 (
    echo Docker build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo Deploying Cloud Run service...
call gcloud run deploy strategiz-core --image gcr.io/strategiz-io/strategiz-core --platform managed --region us-central1 --allow-unauthenticated
if %ERRORLEVEL% NEQ 0 (
    echo Cloud Run deployment failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo Deploying Firebase Hosting for API...
call firebase deploy --only hosting:api
if %ERRORLEVEL% NEQ 0 (
    echo Firebase Hosting deployment failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo Deployment complete!
echo Your API is now available at: https://api-strategiz-io.web.app
echo.
pause
