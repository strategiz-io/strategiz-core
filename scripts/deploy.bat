@echo off
echo ====================================================================
echo Deploying Strategiz Core application...
echo ====================================================================
echo.

REM Check if application JAR exists
if not exist "..\application\target\application-1.0-SNAPSHOT.jar" (
    echo Error: Application JAR not found. 
    echo Please run build.bat first or use build-and-deploy.bat
    exit /b 1
)

echo Starting Strategiz Core application...
echo Press Ctrl+C to stop the application when finished.
echo.

cd ..\application\target
java -jar application-1.0-SNAPSHOT.jar --spring.profiles.active=dev

echo.
echo Strategiz Core application stopped.

REM Return to scripts directory
cd ..\..\scripts
exit /b 0
