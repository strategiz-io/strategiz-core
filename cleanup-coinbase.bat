@echo off
echo Cleaning up old Coinbase code that has been migrated to the new structure...

REM Delete client code that has been migrated
rmdir /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\client"
echo Deleted old client code

REM Delete model code that has been migrated
rmdir /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\model"
echo Deleted old model code

REM Delete controller code that has been migrated
rmdir /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\controller"
echo Deleted old controller code

REM Delete service code that has been migrated
rmdir /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\service"
echo Deleted old service code

REM Delete admin dashboard code that has been migrated
rmdir /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\admindashboard"
echo Deleted old admin dashboard code

REM Delete advanced trade code that has been migrated
rmdir /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\advanced"
echo Deleted old advanced trade code

REM Delete any remaining Coinbase code
rmdir /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase"
echo Deleted any remaining Coinbase code

echo Cleanup complete!
