@echo off
echo ====================================================================
echo Building and deploying Strategiz Core application...
echo ====================================================================
echo.

REM Run the build script
call build.bat
if %ERRORLEVEL% neq 0 (
    echo Build failed with error level %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)

echo.
echo Build completed successfully. Starting deployment...
echo.

REM Run the deploy script
call deploy.bat
if %ERRORLEVEL% neq 0 (
    echo Deployment failed with error level %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)

exit /b 0
