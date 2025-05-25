@echo off
echo Cleaning Maven repository and rebuilding project...

REM Clean the local Maven repository for both old and new groupIds
echo Cleaning io.strategiz artifacts...
rmdir /s /q %USERPROFILE%\.m2\repository\io\strategiz

echo Cleaning strategiz artifacts...
rmdir /s /q %USERPROFILE%\.m2\repository\strategiz

REM Build the parent project first
echo Building parent project...
mvn clean install -N

REM Build the client module
echo Building client module...
cd client
mvn clean install -N
cd ..

REM Build the service module
echo Building service module...
cd service
mvn clean install -N
cd ..

REM Build the api module
echo Building api module...
cd api
mvn clean install -N
cd ..

REM Build the client-coinbase module
echo Building client-coinbase module...
cd client\client-coinbase
mvn clean install -DskipTests
cd ..\..

REM Build the service-exchange module
echo Building service-exchange module...
cd service\service-exchange
mvn clean install -DskipTests
cd ..\..

REM Build the api-exchange module
echo Building api-exchange module...
cd api\api-exchange
mvn clean install -DskipTests
cd ..\..

REM Build the entire project
echo Building entire project...
mvn clean install -DskipTests

echo Build process completed!
