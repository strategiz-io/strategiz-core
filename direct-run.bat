@echo off
echo Building Strategiz Core application...

REM First do a full build to make sure all dependencies are available
call mvn clean install -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

echo.
echo Directly running Strategiz Core application...
echo Press Ctrl+C to stop the application when finished.
echo.

REM Run application directly using java command
cd application
java -cp target/classes;target/dependency/* io.strategiz.application.Application

echo.
echo Strategiz Core application stopped.
