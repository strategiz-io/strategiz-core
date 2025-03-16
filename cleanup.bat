@echo off
echo Starting cleanup of non-Java files...

:: Remove Node.js related files
echo Removing Node.js related files...
del /q package.json
del /q package-lock.json
del /q tsconfig.json
del /q types.d.ts
del /q index.ts

:: Remove JavaScript and TypeScript files in root directories
echo Removing JavaScript and TypeScript files in root directories...
del /s /q *.js
del /s /q *.ts

:: Remove Node.js modules
echo Removing node_modules directory...
rmdir /s /q node_modules

:: Remove legacy route directories
echo Removing legacy route directories...
rmdir /s /q routes
rmdir /s /q binanceus
rmdir /s /q coinbase
rmdir /s /q kraken
rmdir /s /q uniswap

:: Keep essential files
echo Keeping essential build and configuration files...
:: Firebase configuration is likely needed for Java Firebase integration
:: Don't delete firebase-service-account.json

echo Cleanup complete!
echo.
echo Note: This script has removed Node.js, TypeScript, and other non-Java files.
echo The Java backend code and essential configuration files have been preserved.
echo If you need to restore any files, you can use Git to revert specific changes.
