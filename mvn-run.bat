@echo off
echo Starting Strategiz Core application directly with Maven...

cd application
mvn spring-boot:run -Dspring-boot.run.profiles=dev

echo.
echo Strategiz Core application stopped.
