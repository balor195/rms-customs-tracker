@echo off
setlocal enabledelayedexpansion
title RMS Customs - Build Backend EXE
cd /d "%~dp0"

echo ================================================
echo  RMS Customs Sync API - Backend EXE Builder
echo ================================================
echo.

REM -- Locate Python --------------------------------------------------------
where python >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found in PATH.
    echo Please install Python 3.10+ from https://python.org and re-run.
    pause & exit /b 1
)

for /f "tokens=*" %%v in ('python --version 2^>^&1') do set PYVER=%%v
echo [OK] Found %PYVER%

REM -- Virtual environment ---------------------------------------------------
if not exist ".venv\Scripts\activate.bat" (
    echo [INFO] Creating virtual environment...
    python -m venv .venv
    if errorlevel 1 (
        echo [ERROR] Failed to create virtual environment.
        pause & exit /b 1
    )
    echo [OK] Virtual environment created.
)

call .venv\Scripts\activate.bat

REM -- Install / update dependencies ------------------------------------------
echo [INFO] Installing dependencies...
pip install -r requirements.txt --quiet --disable-pip-version-check
if errorlevel 1 (
    echo [ERROR] pip install failed. Check requirements.txt and network connection.
    pause & exit /b 1
)

pip show pyinstaller >nul 2>&1
if errorlevel 1 (
    echo [INFO] Installing PyInstaller...
    pip install pyinstaller --quiet --disable-pip-version-check
    if errorlevel 1 (
        echo [ERROR] Failed to install PyInstaller.
        pause & exit /b 1
    )
)
echo [OK] Dependencies ready.

REM -- Build -------------------------------------------------------------------
echo.
echo [INFO] Building single-file executable...
echo        This may take 30-60 s...
echo.

python -m PyInstaller --onefile --noconfirm --name rms-customs-backend ^
    --hidden-import uvicorn.loops.auto ^
    --hidden-import uvicorn.protocols.http.auto ^
    --hidden-import uvicorn.protocols.websockets.auto ^
    --hidden-import uvicorn.lifespan.on ^
    run_server.py

set "RESULT=%ERRORLEVEL%"

echo.
if %RESULT% neq 0 (
    echo ================================================
    echo  BUILD FAILED  ^(exit code %RESULT%^)
    echo  Scroll up to read the errors.
    echo ================================================
    pause & exit /b %RESULT%
)

REM -- Report success ------------------------------------------------------------
echo ================================================
echo  BUILD SUCCESSFUL
echo  EXE : dist\rms-customs-backend.exe
echo ================================================
echo.

echo [INFO] Opening output folder...
start "" "%~dp0dist"
echo.
pause
