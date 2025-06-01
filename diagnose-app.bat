@echo off
echo ======= STRATEGIZ CORE APPLICATION DIAGNOSTIC =======
echo.
echo Building the application...
call mvn -f application/pom.xml clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Build failed with error code %ERRORLEVEL%!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Running the application with debug logging enabled...
echo Application will be started directly with the jar file...
echo.

cd application\target
echo Command: java -jar -Dlogging.level.root=DEBUG -Dlogging.level.io.strategiz=DEBUG application-1.0-SNAPSHOT.jar
java -jar -Dlogging.level.root=DEBUG -Dlogging.level.io.strategiz=DEBUG application-1.0-SNAPSHOT.jar
set EXIT_CODE=%ERRORLEVEL%

echo.
echo Application exited with code: %EXIT_CODE%
echo.

cd ..\..
echo Diagnostic complete. Press any key to exit...
pause > nul
