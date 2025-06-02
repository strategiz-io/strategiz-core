@echo off
echo ====================================================================
echo Building Strategiz Core in the correct dependency order...
echo ====================================================================
echo.

REM Record the start time
set start_time=%time%

echo Step 1/6: Building framework modules
cd ..\framework\framework-core
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error

cd ..\framework-api-docs
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..\

echo Step 2/6: Building data modules
cd data\data-base
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd data\data-strategy
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd data\data-exchange
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd data\data-portfolio
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd data\data-auth
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo Step 3/6: Building client modules
cd client\client-base
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd client\client-alphavantage
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd client\client-kraken
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd client\client-coinbase
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd client\client-binanceus
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo Step 4/6: Building service modules
cd service\service-base
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd service\service-strategy
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd service\service-exchange
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd service\service-portfolio
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd service\service-dashboard
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd service\service-auth
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo Step 5/6: Building API modules
cd api\api-base
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd api\api-dashboard
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd api\api-exchange
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd api\api-strategy
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd api\api-portfolio
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

cd api\api-auth
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo Step 6/6: Building application module
cd application
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..

REM Return to scripts directory
cd scripts

REM Calculate build time
set end_time=%time%
set options="tokens=1-4 delims=:.,"
for /f %options% %%a in ("%start_time%") do set start_h=%%a&set /a start_m=100%%b %% 100&set /a start_s=100%%c %% 100&set /a start_ms=100%%d %% 100
for /f %options% %%a in ("%end_time%") do set end_h=%%a&set /a end_m=100%%b %% 100&set /a end_s=100%%c %% 100&set /a end_ms=100%%d %% 100

set /a hours=%end_h%-%start_h%
set /a mins=%end_m%-%start_m%
set /a secs=%end_s%-%start_s%
set /a ms=%end_ms%-%start_ms%
if %ms% lss 0 set /a secs = %secs% - 1 & set /a ms = 100%ms%
if %secs% lss 0 set /a mins = %mins% - 1 & set /a secs = 60%secs%
if %mins% lss 0 set /a hours = %hours% - 1 & set /a mins = 60%mins%
if %hours% lss 0 set /a hours = 24%hours%

echo ====================================================================
echo Build completed successfully in %hours%:%mins%:%secs%.%ms% 
echo ====================================================================
exit /b 0

:error
echo ====================================================================
echo Build failed with error level %ERRORLEVEL%
echo ====================================================================
cd ..\..\scripts
exit /b %ERRORLEVEL%
