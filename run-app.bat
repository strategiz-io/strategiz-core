@echo off
echo Building Strategiz Core application...
call mvn -f application/pom.xml clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

echo Starting Strategiz Core application...
java -jar application/target/application-1.0-SNAPSHOT-exec.jar
