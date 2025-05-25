@echo off
echo Cleaning up non-Java files from the repository...

REM Delete JSON files (except those needed for Firebase configuration)
echo Deleting JSON files...
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\firebase.json"

REM Delete HTML files
echo Deleting HTML files...
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\public\index.html"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\public\test.html"

REM Delete the public directory if it's empty
rmdir "C:\Users\cuzto\Documents\GitHub\strategiz-core\public" 2>nul

echo Cleanup of non-Java files complete!
