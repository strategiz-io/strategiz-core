@echo off
echo Building Strategiz Core application...

cd application
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

echo.
echo Starting Strategiz Core application...
echo Press Ctrl+C to stop the application when finished.
echo.

cd target
java -jar application-1.0-SNAPSHOT.jar --spring.profiles.active=dev

echo.
echo Strategiz Core application stopped.
