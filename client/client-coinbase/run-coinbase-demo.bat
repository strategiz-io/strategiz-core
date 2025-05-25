@echo off
echo Running Coinbase API Integration Demo...

REM Compile the project
echo Compiling project...
call mvn compile -f standalone-pom.xml

REM Create test-classes directory if it doesn't exist
if not exist target\test-classes\strategiz\client\coinbase mkdir target\test-classes\strategiz\client\coinbase

REM Compile the demo class
echo Compiling demo class...
javac -cp "target\classes;%USERPROFILE%\.m2\repository\org\springframework\spring-web\5.3.23\spring-web-5.3.23.jar;%USERPROFILE%\.m2\repository\org\springframework\spring-core\5.3.23\spring-core-5.3.23.jar;%USERPROFILE%\.m2\repository\org\springframework\spring-beans\5.3.23\spring-beans-5.3.23.jar;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.13.4.2\jackson-databind-2.13.4.2.jar" -d target\test-classes src\test\java\strategiz\client\coinbase\CoinbaseIntegrationDemo.java

REM Run the demo
echo Running demo...
java -cp "target\classes;target\test-classes;%USERPROFILE%\.m2\repository\org\springframework\spring-web\5.3.23\spring-web-5.3.23.jar;%USERPROFILE%\.m2\repository\org\springframework\spring-core\5.3.23\spring-core-5.3.23.jar;%USERPROFILE%\.m2\repository\org\springframework\spring-beans\5.3.23\spring-beans-5.3.23.jar;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.13.4.2\jackson-databind-2.13.4.2.jar;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.13.4\jackson-annotations-2.13.4.jar;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.13.4\jackson-core-2.13.4.jar;%USERPROFILE%\.m2\repository\org\apache\httpcomponents\httpclient\4.5.13\httpclient-4.5.13.jar;%USERPROFILE%\.m2\repository\org\apache\httpcomponents\httpcore\4.4.13\httpcore-4.4.13.jar;%USERPROFILE%\.m2\repository\commons-logging\commons-logging\1.2\commons-logging-1.2.jar;%USERPROFILE%\.m2\repository\commons-codec\commons-codec\1.11\commons-codec-1.11.jar;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar" strategiz.client.coinbase.CoinbaseIntegrationDemo

echo Demo completed.
