@echo off
echo Building Coinbase Integration Modules...

REM Clean the local Maven repository for strategiz artifacts
echo Cleaning strategiz artifacts from local repository...
rmdir /s /q %USERPROFILE%\.m2\repository\strategiz

REM Build the client-coinbase module directly
echo Building client-coinbase module...
cd client\client-coinbase

REM Create a standalone pom.xml for building just the client-coinbase module
echo Creating standalone pom.xml for client-coinbase...
echo ^<?xml version="1.0" encoding="UTF-8"?^> > standalone-pom.xml
echo ^<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"^> >> standalone-pom.xml
echo     ^<modelVersion^>4.0.0^</modelVersion^> >> standalone-pom.xml
echo     ^<groupId^>strategiz^</groupId^> >> standalone-pom.xml
echo     ^<artifactId^>client-coinbase^</artifactId^> >> standalone-pom.xml
echo     ^<version^>1.0-SNAPSHOT^</version^> >> standalone-pom.xml
echo     ^<name^>Strategiz Coinbase Client^</name^> >> standalone-pom.xml
echo     ^<description^>Coinbase API client for Strategiz platform^</description^> >> standalone-pom.xml
echo     ^<properties^> >> standalone-pom.xml
echo         ^<maven.compiler.source^>11^</maven.compiler.source^> >> standalone-pom.xml
echo         ^<maven.compiler.target^>11^</maven.compiler.target^> >> standalone-pom.xml
echo         ^<project.build.sourceEncoding^>UTF-8^</project.build.sourceEncoding^> >> standalone-pom.xml
echo     ^</properties^> >> standalone-pom.xml
echo     ^<dependencies^> >> standalone-pom.xml
echo         ^<!-- Spring Framework --^> >> standalone-pom.xml
echo         ^<dependency^> >> standalone-pom.xml
echo             ^<groupId^>org.springframework^</groupId^> >> standalone-pom.xml
echo             ^<artifactId^>spring-web^</artifactId^> >> standalone-pom.xml
echo             ^<version^>5.3.23^</version^> >> standalone-pom.xml
echo         ^</dependency^> >> standalone-pom.xml
echo         ^<dependency^> >> standalone-pom.xml
echo             ^<groupId^>org.springframework^</groupId^> >> standalone-pom.xml
echo             ^<artifactId^>spring-context^</artifactId^> >> standalone-pom.xml
echo             ^<version^>5.3.23^</version^> >> standalone-pom.xml
echo         ^</dependency^> >> standalone-pom.xml
echo         ^<!-- Jackson for JSON processing --^> >> standalone-pom.xml
echo         ^<dependency^> >> standalone-pom.xml
echo             ^<groupId^>com.fasterxml.jackson.core^</groupId^> >> standalone-pom.xml
echo             ^<artifactId^>jackson-databind^</artifactId^> >> standalone-pom.xml
echo             ^<version^>2.13.4.2^</version^> >> standalone-pom.xml
echo         ^</dependency^> >> standalone-pom.xml
echo         ^<dependency^> >> standalone-pom.xml
echo             ^<groupId^>com.fasterxml.jackson.core^</groupId^> >> standalone-pom.xml
echo             ^<artifactId^>jackson-annotations^</artifactId^> >> standalone-pom.xml
echo             ^<version^>2.13.4^</version^> >> standalone-pom.xml
echo         ^</dependency^> >> standalone-pom.xml
echo         ^<!-- Apache HttpClient --^> >> standalone-pom.xml
echo         ^<dependency^> >> standalone-pom.xml
echo             ^<groupId^>org.apache.httpcomponents^</groupId^> >> standalone-pom.xml
echo             ^<artifactId^>httpclient^</artifactId^> >> standalone-pom.xml
echo             ^<version^>4.5.13^</version^> >> standalone-pom.xml
echo         ^</dependency^> >> standalone-pom.xml
echo         ^<!-- Lombok --^> >> standalone-pom.xml
echo         ^<dependency^> >> standalone-pom.xml
echo             ^<groupId^>org.projectlombok^</groupId^> >> standalone-pom.xml
echo             ^<artifactId^>lombok^</artifactId^> >> standalone-pom.xml
echo             ^<version^>1.18.24^</version^> >> standalone-pom.xml
echo             ^<scope^>provided^</scope^> >> standalone-pom.xml
echo         ^</dependency^> >> standalone-pom.xml
echo         ^<!-- Logging --^> >> standalone-pom.xml
echo         ^<dependency^> >> standalone-pom.xml
echo             ^<groupId^>org.slf4j^</groupId^> >> standalone-pom.xml
echo             ^<artifactId^>slf4j-api^</artifactId^> >> standalone-pom.xml
echo             ^<version^>1.7.36^</version^> >> standalone-pom.xml
echo         ^</dependency^> >> standalone-pom.xml
echo     ^</dependencies^> >> standalone-pom.xml
echo ^</project^> >> standalone-pom.xml

REM Build the client-coinbase module with the standalone pom.xml
echo Building client-coinbase module with standalone pom.xml...
call mvn clean install -f standalone-pom.xml -DskipTests

cd ..\..

echo Coinbase Integration Modules built successfully!
echo.
echo You can now use the Coinbase client in your application.
