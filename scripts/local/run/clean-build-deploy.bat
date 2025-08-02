@echo off
REM =============================================================================
REM Strategiz Core - Clean Build and Deploy Script (Windows)
REM A modern, efficient build and deploy script using Maven reactor capabilities
REM =============================================================================

setlocal enabledelayedexpansion

REM Change to project root
cd /d "%~dp0..\..\..\"
set PROJECT_ROOT=%CD%

echo ====================================================================
echo   Starting Strategiz Core Clean Build and Deploy
echo ====================================================================
echo Project root: %PROJECT_ROOT%
echo Timestamp: %date% %time%
echo.

REM Pre-build checks
echo ====================================================================
echo   Pre-build Environment Checks
echo ====================================================================

REM Check if Maven is available
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven is not installed or not in PATH
    exit /b 1
)
echo ✅ Maven found

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is not installed or not in PATH
    exit /b 1
)
echo ✅ Java found

REM Check for Vault (optional)
tasklist /FI "IMAGENAME eq vault.exe" 2>NUL | find /I /N "vault.exe" >nul
if %errorlevel% equ 0 (
    echo ✅ Vault server appears to be running
) else (
    echo ⚠️  Vault server doesn't appear to be running
    echo    You may want to start it with: vault server -dev
)

echo.

REM Set Maven options
set MAVEN_OPTS=-Xmx2g -XX:MaxMetaspaceSize=512m

REM Clean build using Maven reactor
echo ====================================================================
echo   Phase 1: Clean Build (Using Maven Reactor)
echo ====================================================================
echo Building all modules in correct dependency order...

mvn clean install -DskipTests -T 1C -q --batch-mode ^
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

if %errorlevel% neq 0 (
    echo ❌ Build failed. Check the output above for details.
    exit /b 1
)

echo ✅ Build completed successfully!
echo.

REM Verify build artifacts
echo ====================================================================
echo   Phase 2: Build Verification
echo ====================================================================

set JAR_FILE=%PROJECT_ROOT%\application\target\application-1.0-SNAPSHOT.jar
if exist "%JAR_FILE%" (
    echo ✅ Application JAR found
) else (
    echo ❌ Application JAR not found at: %JAR_FILE%
    exit /b 1
)

echo.

REM Deploy the application
echo ====================================================================
echo   Phase 3: Application Deployment
echo ====================================================================

if "%VAULT_TOKEN%"=="" (
    echo ⚠️  VAULT_TOKEN not set. Make sure to set it if using Vault
    echo    Example: set VAULT_TOKEN=hvs.your-dev-token
)

echo Starting Strategiz Core application...
echo Application will start with dev profile
echo.
echo 🌐 Backend will be available at: http://localhost:8080
echo 📖 API Documentation: http://localhost:8080/swagger-ui.html
echo 💡 Health Check: http://localhost:8080/actuator/health
echo.
echo ⚠️  Press Ctrl+C to stop the application
echo.

REM Navigate to application directory and start
cd /d "%PROJECT_ROOT%\application\target"

REM Run with dev profile and optimized JVM settings
java -Xmx1g -XX:+UseG1GC ^
     -Dspring.profiles.active=dev ^
     -Dlogging.level.org.springframework.web=INFO ^
     -jar application-1.0-SNAPSHOT.jar

REM Cleanup message
echo.
echo ====================================================================
echo   Application Stopped
echo ====================================================================
echo ✅ Strategiz Core has been stopped cleanly
echo Build artifacts remain in target\ directories for quick restart
echo To restart quickly, run: java -jar "%JAR_FILE%" --spring.profiles.active=dev