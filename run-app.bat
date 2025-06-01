@echo off
echo Building Strategiz Core using ordered build process...
echo.

call build-all.bat
if %ERRORLEVEL% neq 0 (
    echo Build failed with error level %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)

echo.
echo Starting Strategiz Core application...
echo Press Ctrl+C to stop the application when finished.
echo.

cd application\target
java -jar application-1.0-SNAPSHOT.jar --spring.profiles.active=dev

echo.
echo Strategiz Core application stopped.
