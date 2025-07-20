@echo off
REM =============================================================================
REM Strategiz Core - Gradle Clean Build and Deploy Script (Windows)
REM Modern, efficient Gradle-based build and deploy script for Windows
REM =============================================================================

setlocal enabledelayedexpansion

REM Change to project root
cd /d "%~dp0..\..\..\"
set PROJECT_ROOT=%CD%

echo ====================================================================
echo   Strategiz Core - Gradle Build System
echo ====================================================================
echo Project root: %PROJECT_ROOT%
echo Build system: Gradle
echo Timestamp: %date% %time%
echo.

REM Pre-build checks
echo ====================================================================
echo   Pre-build Environment Checks
echo ====================================================================

REM Check if Gradle is available (wrapper or system)
if exist "gradlew.bat" (
    set GRADLE_CMD=gradlew.bat
    echo ✅ Gradle wrapper found (recommended)
) else (
    gradle --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo ❌ Neither Gradle wrapper nor system Gradle found
        echo.
        echo To fix this:
        echo 1. Use Gradle wrapper: gradle wrapper --gradle-version 8.5
        echo 2. Or install Gradle: https://gradle.org/install/
        exit /b 1
    )
    set GRADLE_CMD=gradle
    echo ⚠️  Using system Gradle (wrapper recommended)
)

REM Get Gradle version
%GRADLE_CMD% --version | findstr "Gradle" >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('%GRADLE_CMD% --version ^| findstr "Gradle"') do echo 🐘 Gradle: %%i
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

REM Gradle configuration
echo ====================================================================
echo   Gradle Configuration
echo ====================================================================

REM Set Gradle options
set GRADLE_OPTS=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC
echo 🐘 Gradle: Memory settings configured

REM Check Gradle properties
if exist "gradle.properties" (
    echo 🐘 Gradle: Found gradle.properties
    
    findstr "org.gradle.parallel" gradle.properties >nul 2>&1
    if %errorlevel% equ 0 (
        for /f "tokens=*" %%i in ('findstr "org.gradle.parallel" gradle.properties') do echo 🐘 Gradle: Parallel builds: %%i
    )
    
    findstr "org.gradle.caching" gradle.properties >nul 2>&1
    if %errorlevel% equ 0 (
        for /f "tokens=*" %%i in ('findstr "org.gradle.caching" gradle.properties') do echo 🐘 Gradle: Build cache: %%i
    )
) else (
    echo ⚠️  No gradle.properties found - consider adding for performance
)

REM Validate Gradle configuration
%GRADLE_CMD% help --quiet >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Gradle configuration is valid
) else (
    echo ❌ Gradle build configuration has issues
    exit /b 1
)

echo.

REM Clean build using Gradle
echo ====================================================================
echo   Phase 1: Gradle Clean Build
echo ====================================================================
echo Building with Gradle for dependency management and parallel execution...

set BUILD_LOG=%PROJECT_ROOT%\gradle-build.log
echo Build log: %BUILD_LOG%
echo 🐘 Gradle: Executing clean build with optimizations

%GRADLE_CMD% clean build ^
    -x test ^
    --parallel ^
    --build-cache ^
    --configure-on-demand ^
    --info > "%BUILD_LOG%" 2>&1

if %errorlevel% neq 0 (
    echo ❌ Gradle build failed. Check %BUILD_LOG% for details.
    echo.
    echo 🐘 Gradle: Common build issues:
    echo   • Task dependencies not properly defined
    echo   • Plugin version conflicts
    echo   • Gradle version compatibility
    echo   • Memory issues (increase GRADLE_OPTS^)
    echo   • Network issues (dependencies download^)
    exit /b 1
)

echo ✅ Gradle build completed successfully!

REM Show build time if available
findstr "BUILD SUCCESSFUL in" "%BUILD_LOG%" >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('findstr "BUILD SUCCESSFUL in" "%BUILD_LOG%"') do echo 🐘 Gradle: %%i
)

REM Show cache performance
findstr /c:"cache hit" "%BUILD_LOG%" >nul 2>&1
if %errorlevel% equ 0 (
    for /f %%i in ('findstr /c:"cache hit" "%BUILD_LOG%" ^| find /c /v ""') do echo 🐘 Gradle: Build cache hits: %%i
)

echo.

REM Gradle build verification
echo ====================================================================
echo   Phase 2: Gradle Build Verification
echo ====================================================================

set JAR_FILE=%PROJECT_ROOT%\application\build\libs\application-1.0-SNAPSHOT.jar
if exist "%JAR_FILE%" (
    echo ✅ Application JAR created
) else (
    REM Check alternative location
    set ALT_JAR=%PROJECT_ROOT%\application\build\libs\application.jar
    if exist "!ALT_JAR!" (
        set JAR_FILE=!ALT_JAR!
        echo ⚠️  Found JAR at alternative location: !ALT_JAR!
    ) else (
        echo ❌ Application JAR not found
        exit /b 1
    )
)

REM Verify JAR structure
jar -tf "%JAR_FILE%" | findstr "BOOT-INF" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Spring Boot JAR structure verified
) else (
    echo ⚠️  JAR may not be a proper Spring Boot executable JAR
)

REM Check framework modules (if using Gradle multi-project)
if exist "framework" (
    for %%m in (framework-secrets framework-exception framework-logging) do (
        if exist "framework\%%m\build\libs\%%m-1.0-SNAPSHOT.jar" (
            echo ✅ Framework module: %%m
        ) else (
            echo ⚠️  Framework module JAR not found: %%m (may not be configured for Gradle^)
        )
    )
)

echo.

REM Optional analysis
set /p ANALYSIS="Do you want to run Gradle build analysis? (y/N): "
if /i "%ANALYSIS%"=="y" (
    echo ====================================================================
    echo   Gradle Build Analysis
    echo ====================================================================
    echo 🐘 Gradle: Generating reports...
    %GRADLE_CMD% dependencies --configuration compileClasspath > gradle-dependencies.txt 2>nul
    %GRADLE_CMD% projects > gradle-projects.txt 2>nul
    echo ✅ Reports saved to gradle-dependencies.txt and gradle-projects.txt
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

echo Starting Strategiz Core application...
echo Built with Gradle build system
echo.
echo 🌐 Backend: http://localhost:8080
echo 📖 API Docs: http://localhost:8080/swagger-ui.html
echo 💡 Health: http://localhost:8080/actuator/health
echo 🐘 Build Tool: Gradle
echo.
echo ⚠️  Press Ctrl+C to stop the application
echo.

REM Navigate to Gradle build output directory
for %%i in ("%JAR_FILE%") do (
    cd /d "%%~dpi"
    set JAR_NAME=%%~nxi
)

REM Run with optimized settings
java -Xmx1g -XX:+UseG1GC ^
     -Dspring.profiles.active=dev ^
     -Dlogging.level.org.springframework.web=INFO ^
     -Dgradle.build.tool=gradle ^
     -jar "%JAR_NAME%"

REM Cleanup message
echo.
echo ====================================================================
echo   Application Stopped
echo ====================================================================
echo ✅ Strategiz Core (Gradle build) has been stopped cleanly
echo Gradle artifacts available in build\ directories
echo Build log: %BUILD_LOG%
echo.
echo 🐘 Gradle: Quick restart: java -jar "%JAR_FILE%" --spring.profiles.active=dev