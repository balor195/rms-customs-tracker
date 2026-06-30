@echo off
setlocal enabledelayedexpansion
title RMS Customs — Backend Server
cd /d "%~dp0"

echo ================================================
echo  RMS Customs Clearance — FastAPI Backend
echo  مديرية الصيدلة والتجهيزات الطبية
echo ================================================
echo.

:: ── Check Python ─────────────────────────────────────────────────────────────
where python >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found in PATH.
    echo Please install Python 3.10+ from https://python.org and re-run.
    pause & exit /b 1
)

for /f "tokens=*" %%v in ('python --version 2^>^&1') do set PYVER=%%v
echo [OK] Found %PYVER%

:: ── Virtual environment ───────────────────────────────────────────────────────
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

:: ── Install / update dependencies ─────────────────────────────────────────────
echo [INFO] Installing dependencies from requirements.txt...
pip install -r requirements.txt --quiet --disable-pip-version-check
if errorlevel 1 (
    echo [ERROR] pip install failed. Check requirements.txt and network connection.
    pause & exit /b 1
)
echo [OK] Dependencies ready.

:: ── Start server ──────────────────────────────────────────────────────────────
echo.
echo ================================================
echo  Server starting on http://0.0.0.0:8000
echo  Swagger UI : http://localhost:8000/docs
echo  Health     : http://localhost:8000/health
echo  Emulator   : http://10.0.2.2:8000
echo  Press Ctrl+C to stop.
echo ================================================
echo.

python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload

echo.
echo [INFO] Server stopped.
pause
