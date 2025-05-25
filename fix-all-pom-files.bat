@echo off
echo Fixing all POM files with correct groupId and parent references...

REM Fix data module POM files
echo Fixing data module POM files...

cd data\data-strategy
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>data</artifactId>', '<artifactId>data</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

cd data\data-portfolio
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>data</artifactId>', '<artifactId>data</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

cd data\data-auth
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>data</artifactId>', '<artifactId>data</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

REM Fix service module POM files
echo Fixing service module POM files...

cd service\service-strategy
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>service</artifactId>', '<artifactId>service</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

cd service\service-portfolio
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>service</artifactId>', '<artifactId>service</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

cd service\service-auth
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>service</artifactId>', '<artifactId>service</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

REM Fix API module POM files
echo Fixing API module POM files...

cd api
powershell -Command "(Get-Content pom.xml) -replace '<n>', '<name>' | Set-Content pom.xml"
cd ..

cd api\api-strategy
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>api</artifactId>', '<artifactId>api</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

cd api\api-portfolio
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>api</artifactId>', '<artifactId>api</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

cd api\api-auth
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>api</artifactId>', '<artifactId>api</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

cd api\api-monitoring
powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
powershell -Command "(Get-Content pom.xml) -replace '<artifactId>api</artifactId>', '<artifactId>api</artifactId>\n        <relativePath>../pom.xml</relativePath>' | Set-Content pom.xml"
cd ..\..

REM Fix any remaining <n> tags to <name> tags
echo Fixing any remaining <n> tags to <name> tags...
powershell -Command "Get-ChildItem -Path . -Filter pom.xml -Recurse | ForEach-Object { (Get-Content $_.FullName) -replace '<n>', '<name>' | Set-Content $_.FullName }"

REM Fix any remaining io.strategiz references in dependencies
echo Fixing any remaining io.strategiz references in dependencies...
powershell -Command "Get-ChildItem -Path . -Filter pom.xml -Recurse | ForEach-Object { (Get-Content $_.FullName) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content $_.FullName }"

echo All POM files have been fixed!
