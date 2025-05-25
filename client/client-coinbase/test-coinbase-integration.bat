@echo off
echo Testing Coinbase API Integration

REM Compile the project
echo Compiling project...
call mvn clean compile

REM Run the tester class
echo Running Coinbase tester...
call mvn exec:java -Dexec.mainClass="strategiz.client.coinbase.CoinbaseTester" -Dexec.classpathScope=test

echo Test completed.
