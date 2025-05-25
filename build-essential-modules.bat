@echo off
echo Building essential modules for Coinbase integration...

REM Clean the local Maven repository for strategiz artifacts
echo Cleaning strategiz artifacts...
rmdir /s /q %USERPROFILE%\.m2\repository\strategiz

REM Build the parent project first
echo Building parent project...
call mvn clean install -N
if %ERRORLEVEL% neq 0 goto :error

REM Build the client module parent
echo Building client module parent...
cd client
call mvn clean install -N
if %ERRORLEVEL% neq 0 goto :error
cd ..

REM Build the client-coinbase module
echo Building client-coinbase module...
cd client\client-coinbase
call mvn clean install -DskipTests
if %ERRORLEVEL% neq 0 goto :error
cd ..\..

echo All essential modules built successfully!
goto :end

:error
echo Build failed with error %ERRORLEVEL%
exit /b %ERRORLEVEL%

:end
