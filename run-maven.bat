@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using JAVA_HOME: %JAVA_HOME%
echo.
call mvn clean install
pause
