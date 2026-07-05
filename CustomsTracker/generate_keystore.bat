@echo off
setlocal enabledelayedexpansion
title RMS Customs — Generate Release Keystore
cd /d "%~dp0"

echo ================================================
echo  RMS Customs Clearance ^| Release Keystore Setup
echo ================================================
echo.

if exist "keystore.properties" (
    echo [WARN] keystore.properties already exists.
    echo Re-running this will overwrite it, and any APK you have
    echo already shipped will no longer match a new keystore.
    echo.
    set /p "CONFIRM=Overwrite existing signing key? [y/N]: "
    if /i not "!CONFIRM!"=="y" (
        echo Aborted. Existing keystore left untouched.
        pause & exit /b 0
    )
)

:: ── Locate JDK keytool ────────────────────────────────────────────────────────
set "KEYTOOL="
if exist "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" (
    set "KEYTOOL=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
) else if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\keytool.exe" (
    set "KEYTOOL=%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\keytool.exe"
) else if exist "C:\Program Files\Java\jdk-17\bin\keytool.exe" (
    set "KEYTOOL=C:\Program Files\Java\jdk-17\bin\keytool.exe"
)

if "!KEYTOOL!"=="" (
    echo [ERROR] Could not locate keytool.exe ^(ships with the JDK / Android Studio^).
    pause & exit /b 1
)
echo [OK] keytool : !KEYTOOL!
echo.

:: ── Collect key details ───────────────────────────────────────────────────────
set "KEYSTORE_FILE=rms-customs-release.jks"
set "ALIAS=rms-customs"

echo This key identifies your app for ALL future updates.
echo Losing it means you can never publish an update under the same app.
echo Keep "%KEYSTORE_FILE%" and the password backed up somewhere safe
echo ^(password manager, offline backup — NOT just this PC^).
echo.
set /p "STOREPASS=Enter a strong keystore password: "
if "!STOREPASS!"=="" (
    echo [ERROR] Password cannot be empty.
    pause & exit /b 1
)

if exist "%KEYSTORE_FILE%" del /f /q "%KEYSTORE_FILE%"

"!KEYTOOL!" -genkeypair -v ^
    -keystore "%KEYSTORE_FILE%" ^
    -alias "%ALIAS%" ^
    -keyalg RSA -keysize 2048 -validity 10000 ^
    -storepass "!STOREPASS!" ^
    -dname "CN=RMS Customs Tracker, OU=IT, O=Royal Medical Services, L=Amman, ST=Amman, C=JO"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Key generation failed.
    pause & exit /b 1
)

> "keystore.properties" (
    echo storeFile=%KEYSTORE_FILE%
    echo storePassword=!STOREPASS!
    echo keyAlias=%ALIAS%
    echo keyPassword=!STOREPASS!
)

echo.
echo ================================================
echo  KEYSTORE CREATED
echo  File : %KEYSTORE_FILE%
echo  Alias: %ALIAS%
echo ================================================
echo.
echo [INFO] keystore.properties written ^(git-ignored — never commit it^).
echo [INFO] Run build_apk.bat and choose option 2 to build a signed release APK.
echo.
pause
