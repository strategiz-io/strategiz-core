@echo off
echo Cleaning up old structure and migrating remaining Java files...

REM Create necessary directories in the new structure
echo Creating necessary directories in the new structure...
mkdir -p api\api-common\src\main\java\strategiz\api\common\model
mkdir -p api\api-common\src\main\java\strategiz\api\common\config
mkdir -p api\api-common\src\main\java\strategiz\api\common\controller
mkdir -p service\service-common\src\main\java\strategiz\service\common\model
mkdir -p service\service-marketplace\src\main\java\strategiz\service\marketplace\model
mkdir -p service\service-marketplace\src\main\java\strategiz\service\marketplace\controller
mkdir -p client\client-walletaddress\src\main\java\strategiz\client\walletaddress\model
mkdir -p client\client-walletaddress\src\main\java\strategiz\client\walletaddress\client
mkdir -p service\service-walletaddress\src\main\java\strategiz\service\walletaddress\service

REM Migrate common model files
echo Migrating common model files...
copy "src\main\java\io\strategiz\common\model\ApiResponse.java" "api\api-common\src\main\java\strategiz\api\common\model\ApiResponse.java"

REM Migrate config files
echo Migrating config files...
copy "src\main\java\io\strategiz\config\CorsFilter.java" "api\api-common\src\main\java\strategiz\api\common\config\CorsFilter.java"
copy "src\main\java\io\strategiz\config\FirebaseConfig.java" "api\api-common\src\main\java\strategiz\api\common\config\FirebaseConfig.java"
copy "src\main\java\io\strategiz\config\SecurityConfig.java" "api\api-common\src\main\java\strategiz\api\common\config\SecurityConfig.java"
copy "src\main\java\io\strategiz\config\WebConfig.java" "api\api-common\src\main\java\strategiz\api\common\config\WebConfig.java"

REM Migrate controller files
echo Migrating controller files...
for %%f in (src\main\java\io\strategiz\controller\*.java) do (
    copy "%%f" "api\api-common\src\main\java\strategiz\api\common\controller\"
)

REM Migrate framework files
echo Migrating framework files...
for %%f in (src\main\java\io\strategiz\framework\*.java) do (
    copy "%%f" "api\api-common\src\main\java\strategiz\api\common\"
)
for %%f in (src\main\java\io\strategiz\framework\rest\*.java) do (
    copy "%%f" "api\api-common\src\main\java\strategiz\api\common\"
)
for %%f in (src\main\java\io\strategiz\framework\rest\controller\*.java) do (
    copy "%%f" "api\api-common\src\main\java\strategiz\api\common\controller\"
)
for %%f in (src\main\java\io\strategiz\framework\rest\model\*.java) do (
    copy "%%f" "api\api-common\src\main\java\strategiz\api\common\model\"
)

REM Migrate marketplace files
echo Migrating marketplace files...
for %%f in (src\main\java\io\strategiz\marketplace\*.java) do (
    copy "%%f" "service\service-marketplace\src\main\java\strategiz\service\marketplace\"
)
for %%f in (src\main\java\io\strategiz\marketplace\controller\*.java) do (
    copy "%%f" "service\service-marketplace\src\main\java\strategiz\service\marketplace\controller\"
)
for %%f in (src\main\java\io\strategiz\marketplace\model\*.java) do (
    copy "%%f" "service\service-marketplace\src\main\java\strategiz\service\marketplace\model\"
)

REM Migrate walletaddress files
echo Migrating walletaddress files...
for %%f in (src\main\java\io\strategiz\walletaddress\*.java) do (
    copy "%%f" "service\service-walletaddress\src\main\java\strategiz\service\walletaddress\"
)
for %%f in (src\main\java\io\strategiz\walletaddress\client\*.java) do (
    copy "%%f" "client\client-walletaddress\src\main\java\strategiz\client\walletaddress\client\"
)
for %%f in (src\main\java\io\strategiz\walletaddress\model\*.java) do (
    copy "%%f" "client\client-walletaddress\src\main\java\strategiz\client\walletaddress\model\"
)
for %%f in (src\main\java\io\strategiz\walletaddress\service\*.java) do (
    copy "%%f" "service\service-walletaddress\src\main\java\strategiz\service\walletaddress\service\"
)

REM Migrate any remaining trading agent files
echo Migrating any remaining trading agent files...
for %%f in (src\main\java\io\strategiz\trading\*.java) do (
    copy "%%f" "service\service-exchange\src\main\java\strategiz\service\exchange\trading\"
)

REM Update package declarations in the migrated files
echo Updating package declarations in the migrated files...

REM Common model files
if exist "api\api-common\src\main\java\strategiz\api\common\model\ApiResponse.java" (
    powershell -Command "(Get-Content 'api\api-common\src\main\java\strategiz\api\common\model\ApiResponse.java') -replace 'package io.strategiz.common.model', 'package strategiz.api.common.model' | Set-Content 'api\api-common\src\main\java\strategiz\api\common\model\ApiResponse.java'"
)

REM Config files
if exist "api\api-common\src\main\java\strategiz\api\common\config\CorsFilter.java" (
    powershell -Command "(Get-Content 'api\api-common\src\main\java\strategiz\api\common\config\CorsFilter.java') -replace 'package io.strategiz.config', 'package strategiz.api.common.config' | Set-Content 'api\api-common\src\main\java\strategiz\api\common\config\CorsFilter.java'"
)
if exist "api\api-common\src\main\java\strategiz\api\common\config\FirebaseConfig.java" (
    powershell -Command "(Get-Content 'api\api-common\src\main\java\strategiz\api\common\config\FirebaseConfig.java') -replace 'package io.strategiz.config', 'package strategiz.api.common.config' | Set-Content 'api\api-common\src\main\java\strategiz\api\common\config\FirebaseConfig.java'"
)
if exist "api\api-common\src\main\java\strategiz\api\common\config\SecurityConfig.java" (
    powershell -Command "(Get-Content 'api\api-common\src\main\java\strategiz\api\common\config\SecurityConfig.java') -replace 'package io.strategiz.config', 'package strategiz.api.common.config' | Set-Content 'api\api-common\src\main\java\strategiz\api\common\config\SecurityConfig.java'"
)
if exist "api\api-common\src\main\java\strategiz\api\common\config\WebConfig.java" (
    powershell -Command "(Get-Content 'api\api-common\src\main\java\strategiz\api\common\config\WebConfig.java') -replace 'package io.strategiz.config', 'package strategiz.api.common.config' | Set-Content 'api\api-common\src\main\java\strategiz\api\common\config\WebConfig.java'"
)

echo Deleting the old structure...
rmdir /s /q "src\main\java\io\strategiz"

echo Cleanup and migration complete!
