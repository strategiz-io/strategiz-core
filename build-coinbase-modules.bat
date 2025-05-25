@echo off
echo Building Coinbase-related modules...

REM Clean the Maven repository for strategiz artifacts
echo Cleaning strategiz artifacts...
rmdir /s /q %USERPROFILE%\.m2\repository\strategiz

REM Build the parent project first
echo Building parent project...
mvn clean install -N
if %ERRORLEVEL% neq 0 goto :error

REM Build the client module
echo Building client module...
cd client
mvn clean install -N
if %ERRORLEVEL% neq 0 goto :error
cd ..

REM Build the client-coinbase module
echo Building client-coinbase module...
cd client\client-coinbase
mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

REM Build the service module
echo Building service module...
cd service
mvn clean install -N
if %ERRORLEVEL% neq 0 goto :error
cd ..

REM Build the service-common module
echo Building service-common module...
cd service\service-common
mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

REM Build the service-exchange module
echo Building service-exchange module...
cd service\service-exchange
mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo All modules built successfully!
goto :end

:error
echo Build failed with error %ERRORLEVEL%
exit /b %ERRORLEVEL%

:end
