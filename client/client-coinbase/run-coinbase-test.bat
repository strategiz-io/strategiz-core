@echo off
echo Running Coinbase Client Integration Test...

REM Compile the test class
echo Compiling test class...
call mvn compile -f standalone-pom.xml

REM Create test-classes directory if it doesn't exist
if not exist target\test-classes mkdir target\test-classes

REM Copy the test class to test-classes directory
echo Copying test class...
xcopy /Y target\classes\strategiz\client\coinbase\*.class target\test-classes\strategiz\client\coinbase\

REM Run the test
echo Running test...
call mvn exec:java -f standalone-pom.xml -Dexec.mainClass="strategiz.client.coinbase.CoinbaseClientTest" -Dexec.classpathScope=test

echo Test completed.
