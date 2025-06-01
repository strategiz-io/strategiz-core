@echo off
echo Building Strategiz Core in the correct order...

echo Step 1/6: Building framework modules
call mvn clean install -DskipTests -f ../framework/framework-core/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../framework/framework-api-docs/pom.xml
if %ERRORLEVEL% neq 0 goto :error

echo Step 2/6: Building data modules
call mvn clean install -DskipTests -f ../data/data-base/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../data/data-strategy/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../data/data-exchange/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../data/data-portfolio/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../data/data-auth/pom.xml
if %ERRORLEVEL% neq 0 goto :error

echo Step 3/6: Building client modules
call mvn clean install -DskipTests -f ../client/client-base/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../client/client-alphavantage/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../client/client-binanceus/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../client/client-coinbase/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../client/client-coingecko/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../client/client-kraken/pom.xml
if %ERRORLEVEL% neq 0 goto :error

echo Step 4/6: Building service modules
call mvn clean install -DskipTests -f ../service/service-base/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../service/service-strategy/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../service/service-portfolio/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../service/service-exchange/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../service/service-auth/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../service/service-dashboard/pom.xml
if %ERRORLEVEL% neq 0 goto :error

echo Step 5/6: Building API modules
call mvn clean install -DskipTests -f ../api/api-base/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../api/api-auth/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../api/api-exchange/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../api/api-monitoring/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../api/api-portfolio/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../api/api-strategy/pom.xml
if %ERRORLEVEL% neq 0 goto :error

call mvn clean install -DskipTests -f ../api/api-dashboard/pom.xml
if %ERRORLEVEL% neq 0 goto :error

echo Step 6/6: Building application module
call mvn clean install -DskipTests -f ../application/pom.xml
if %ERRORLEVEL% neq 0 goto :error

echo Build completed successfully!
goto :end

:error
echo Build failed with error code %ERRORLEVEL%
exit /b %ERRORLEVEL%

:end
