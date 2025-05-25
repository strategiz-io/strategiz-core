@echo off
echo Updating POM files to use the new groupId and add relativePath...

REM Update data module POM files
cd data
if exist pom.xml (
    echo Updating data/pom.xml
    powershell -Command "(Get-Content pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content pom.xml"
)

for /d %%d in (data-*) do (
    if exist %%d\pom.xml (
        echo Updating %%d\pom.xml
        powershell -Command "(Get-Content %%d\pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content %%d\pom.xml"
        powershell -Command "(Get-Content %%d\pom.xml) -replace '<parent>[^<]*<artifactId>data</artifactId>[^<]*<version>[^<]*</version>[^<]*</parent>', '<parent>$&$nl        <relativePath>../pom.xml</relativePath>$nl    </parent>' -replace '$nl$nl', '$nl'" | Set-Content %%d\pom.xml"
    )
)
cd ..

REM Update api module POM files
cd api
for /d %%d in (api-*) do (
    if exist %%d\pom.xml (
        echo Updating %%d\pom.xml
        powershell -Command "(Get-Content %%d\pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content %%d\pom.xml"
        powershell -Command "(Get-Content %%d\pom.xml) -replace '<parent>[^<]*<artifactId>api</artifactId>[^<]*<version>[^<]*</version>[^<]*</parent>', '<parent>$&$nl        <relativePath>../pom.xml</relativePath>$nl    </parent>' -replace '$nl$nl', '$nl'" | Set-Content %%d\pom.xml"
    )
)
cd ..

REM Update service module POM files
cd service
for /d %%d in (service-*) do (
    if exist %%d\pom.xml (
        echo Updating %%d\pom.xml
        powershell -Command "(Get-Content %%d\pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content %%d\pom.xml"
        powershell -Command "(Get-Content %%d\pom.xml) -replace '<parent>[^<]*<artifactId>service</artifactId>[^<]*<version>[^<]*</version>[^<]*</parent>', '<parent>$&$nl        <relativePath>../pom.xml</relativePath>$nl    </parent>' -replace '$nl$nl', '$nl'" | Set-Content %%d\pom.xml"
    )
)
cd ..

REM Update client module POM files
cd client
for /d %%d in (client-*) do (
    if exist %%d\pom.xml (
        echo Updating %%d\pom.xml
        powershell -Command "(Get-Content %%d\pom.xml) -replace '<groupId>io.strategiz</groupId>', '<groupId>strategiz</groupId>' | Set-Content %%d\pom.xml"
        powershell -Command "(Get-Content %%d\pom.xml) -replace '<parent>[^<]*<artifactId>client</artifactId>[^<]*<version>[^<]*</version>[^<]*</parent>', '<parent>$&$nl        <relativePath>../pom.xml</relativePath>$nl    </parent>' -replace '$nl$nl', '$nl'" | Set-Content %%d\pom.xml"
    )
)
cd ..

echo All POM files updated successfully!
