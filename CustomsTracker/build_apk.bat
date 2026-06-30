@echo off
setlocal enabledelayedexpansion
title RMS Customs — Build APK
cd /d "%~dp0"

echo ================================================
echo  RMS Customs Clearance ^| Android APK Builder
echo  الخدمات الطبية الملكية
echo ================================================
echo.

:: ── Locate JDK ───────────────────────────────────────────────────────────────
set JAVA_HOME=

if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    goto :java_found
)
if exist "C:\Program Files\Android\Android Studio\jre\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jre"
    goto :java_found
)
if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
    goto :java_found
)
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    goto :java_found
)
if exist "C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17"
    goto :java_found
)

echo [ERROR] Could not locate a JDK automatically.
echo.
echo Please set JAVA_HOME manually, e.g.:
echo   set JAVA_HOME=C:\path\to\jdk
echo Then re-run this script.
pause & exit /b 1

:java_found
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [OK] JDK : %JAVA_HOME%

:: ── Verify gradlew ────────────────────────────────────────────────────────────
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found.
    echo Run this script from the CustomsTracker project root.
    pause & exit /b 1
)

:: ── Choose build type ─────────────────────────────────────────────────────────
echo.
echo  Select build type:
echo    1  Debug   ^(fast, unsigned — for testing^)   [default]
echo    2  Release ^(minified, unsigned^)
echo.
set "CHOICE=1"
set /p "INPUT=Enter 1 or 2 [default=1]: "
if not "!INPUT!"=="" set "CHOICE=!INPUT!"

if "!CHOICE!"=="2" (
    set "BUILD_TASK=assembleRelease"
    set "APK_SUBDIR=app\build\outputs\apk\release"
    set "APK_FILE=app-release-unsigned.apk"
) else (
    set "BUILD_TASK=assembleDebug"
    set "APK_SUBDIR=app\build\outputs\apk\debug"
    set "APK_FILE=app-debug.apk"
)

:: ── Build ─────────────────────────────────────────────────────────────────────
echo.
echo [INFO] Running: gradlew %BUILD_TASK%
echo        This may take 30–60 s on the first run...
echo.

call gradlew.bat %BUILD_TASK% --console=plain
set "RESULT=%ERRORLEVEL%"

echo.
if %RESULT% neq 0 (
    echo ================================================
    echo  BUILD FAILED  ^(exit code %RESULT%^)
    echo  Scroll up to read the compiler errors.
    echo ================================================
    pause & exit /b %RESULT%
)

:: ── Report success ────────────────────────────────────────────────────────────
set "APK_PATH=%~dp0%APK_SUBDIR%\%APK_FILE%"

echo ================================================
echo  BUILD SUCCESSFUL
echo  APK : %APK_SUBDIR%\%APK_FILE%
echo ================================================
echo.

:: ── Optional: ADB install ─────────────────────────────────────────────────────
where adb >nul 2>&1
if %ERRORLEVEL% equ 0 (
    for /f "skip=1 tokens=1,2" %%a in ('adb devices 2^>nul') do (
        if "%%b"=="device" (
            set "DEV_CONNECTED=1"
        )
    )
)

if defined DEV_CONNECTED (
    echo [INFO] Android device or emulator detected.
    set "INSTALL=Y"
    set /p "INST_INPUT=Install to device now? [Y/n]: "
    if not "!INST_INPUT!"=="" set "INSTALL=!INST_INPUT!"
    if /i not "!INSTALL!"=="n" (
        echo [INFO] Installing...
        adb install -r "%APK_PATH%"
        if %ERRORLEVEL% equ 0 (
            echo [OK]   Installed successfully.
        ) else (
            echo [WARN] adb install failed. You can sideload manually.
        )
    )
) else (
    echo [INFO] No device connected — skipping install.
)

:: ── Open folder ───────────────────────────────────────────────────────────────
echo.
echo [INFO] Opening output folder...
start "" "%~dp0%APK_SUBDIR%"
echo.
pause
