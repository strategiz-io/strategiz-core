@echo off
echo Running Coinbase Real-Time Price Demo...

REM Compile the project
echo Compiling project...
call mvn compile -f standalone-pom.xml

REM Run the demo
echo Running demo...
call mvn exec:java -f standalone-pom.xml -Dexec.mainClass="strategiz.client.coinbase.demo.CoinbasePriceDemo"

echo Demo completed.
