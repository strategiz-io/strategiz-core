@echo off
REM =============================================================================
REM Strategiz Core - Maven Clean Build and Deploy Script (Windows)
REM Modern, efficient Maven-based build and deploy script for Windows
REM =============================================================================

setlocal enabledelayedexpansion

REM Change to project root
cd /d "%~dp0..\..\..\"
set PROJECT_ROOT=%CD%

echo ====================================================================
echo   Strategiz Core - Maven Build System
echo ====================================================================
echo Project root: %PROJECT_ROOT%
echo Build system: Apache Maven
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

REM Check Maven wrapper
if exist "mvnw.cmd" (
    set MVN_CMD=mvnw.cmd
    echo 🔨 Maven: Using Maven wrapper
) else (
    set MVN_CMD=mvn
    echo 🔨 Maven: Using system Maven
)

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
)

echo.

REM Maven configuration
echo ====================================================================
echo   Maven Configuration
echo ====================================================================

REM Set Maven options
set MAVEN_OPTS=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC
echo 🔨 Maven: Memory settings configured

REM Validate Maven configuration
%MVN_CMD% help:effective-pom -q -N >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Maven configuration is valid
) else (
    echo ❌ Maven POM configuration has issues
    exit /b 1
)

echo.

REM Clean build using Maven reactor
echo ====================================================================
echo   Phase 1: Maven Clean Build
echo ====================================================================
echo Building with Maven reactor for optimal dependency resolution...

set BUILD_LOG=%PROJECT_ROOT%\maven-build.log
echo Build log: %BUILD_LOG%
echo 🔨 Maven: Executing clean install with optimizations

%MVN_CMD% clean install ^
    -DskipTests ^
    -T 1C ^
    --batch-mode ^
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn ^
    -Dmaven.compile.fork=true ^
    -Dmaven.compiler.maxmem=1024m > "%BUILD_LOG%" 2>&1

if %errorlevel% neq 0 (
    echo ❌ Maven build failed. Check %BUILD_LOG% for details.
    echo.
    echo 🔨 Maven: Common build issues:
    echo   • Module dependency conflicts
    echo   • Compilation errors  
    echo   • Missing dependencies
    echo   • Memory issues (increase MAVEN_OPTS^)
    exit /b 1
)

echo ✅ Maven build completed successfully!

REM Show build time if available
findstr "Total time:" "%BUILD_LOG%" >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('findstr "Total time:" "%BUILD_LOG%"') do echo 🔨 Maven: %%i
)

echo.

REM Maven build verification
echo ====================================================================
echo   Phase 2: Maven Build Verification
echo ====================================================================

set JAR_FILE=%PROJECT_ROOT%\application\target\application-1.0-SNAPSHOT.jar
if exist "%JAR_FILE%" (
    echo ✅ Application JAR created
    
    REM Verify JAR structure
    jar -tf "%JAR_FILE%" | findstr "BOOT-INF" >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ Spring Boot JAR structure verified
    ) else (
        echo ⚠️  JAR may not be a proper Spring Boot executable JAR
    )
) else (
    echo ❌ Application JAR not found at: %JAR_FILE%
    exit /b 1
)

REM Check framework modules
set MODULES=framework-secrets framework-exception framework-logging
for %%m in (%MODULES%) do (
    if exist "framework\%%m\target\%%m-1.0-SNAPSHOT.jar" (
        echo ✅ Framework module: %%m
    ) else (
        echo ⚠️  Framework module JAR not found: %%m
    )
)

echo.

REM Optional dependency analysis
set /p ANALYSIS="Do you want to run Maven dependency analysis? (y/N): "
if /i "%ANALYSIS%"=="y" (
    echo ====================================================================
    echo   Maven Dependency Analysis
    echo ====================================================================
    echo 🔨 Maven: Generating dependency tree...
    %MVN_CMD% dependency:tree -Doutput=maven-dependencies.txt >nul 2>&1
    echo ✅ Dependency tree saved to maven-dependencies.txt
    echo.
)

REM Deploy the application
echo ====================================================================
echo   Phase 3: Application Deployment
echo ====================================================================

if "%VAULT_TOKEN%"=="" (
    echo ⚠️  VAULT_TOKEN not set. Make sure to set it if using Vault
    echo    Example: set VAULT_TOKEN=hvs.your-dev-token
)

REM Show active profiles
%MVN_CMD% help:active-profiles -q 2>nul | findstr "Active Profiles" >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('%MVN_CMD% help:active-profiles -q ^| findstr "Active Profiles"') do echo 🔨 Maven: %%i
)

echo Starting Strategiz Core application...
echo Built with Maven reactor
echo.
echo 🌐 Backend: http://localhost:8080
echo 📖 API Docs: http://localhost:8080/swagger-ui.html
echo 💡 Health: http://localhost:8080/actuator/health
echo 🔨 Build Tool: Maven
echo.
echo ⚠️  Press Ctrl+C to stop the application
echo.

REM Navigate to application directory and start
cd /d "%PROJECT_ROOT%\application\target"

REM Run with optimized settings
java -Xmx1g -XX:+UseG1GC ^
     -Dspring.profiles.active=dev ^
     -Dlogging.level.org.springframework.web=INFO ^
     -Dmaven.build.tool=maven ^
     -jar application-1.0-SNAPSHOT.jar

REM Cleanup message
echo.
echo ====================================================================
echo   Application Stopped
echo ====================================================================
echo ✅ Strategiz Core (Maven build) has been stopped cleanly
echo Maven artifacts available in target\ directories
echo Build log: %BUILD_LOG%
echo.
echo 🔨 Maven: Quick restart: java -jar "%JAR_FILE%" --spring.profiles.active=dev