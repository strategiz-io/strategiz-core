@echo off
echo Cleaning all modules...
call mvn clean

echo Building client-coinbase module...
call mvn install -DskipTests -pl client/client-coinbase

echo Building service-exchange module...
call mvn install -DskipTests -pl service/service-exchange

echo Building api-exchange module...
call mvn install -DskipTests -pl api/api-exchange

echo Build complete!
