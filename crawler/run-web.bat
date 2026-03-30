@echo off
setlocal

REM Navigate to the directory of this script
cd /d "%~dp0"

REM Move into the web folder
if not exist "web" (
	echo Folder "web" not found in %cd%.
	pause
	exit /b 1
)
cd web

REM Ensure Node.js is installed
where node >nul 2>nul
if errorlevel 1 (
	echo Node.js is not installed or not in PATH.
	echo Please install from https://nodejs.org/
	pause
	exit /b 1
)

REM Always install/update dependencies (fast if already up-to-date)
echo Installing npm dependencies...
call npm install
if errorlevel 1 (
	echo npm install failed.
	pause
	exit /b 1
)

REM Start Vite dev server and open browser
echo Starting development server...
call npm run dev -- --open
if errorlevel 1 (
	echo Development server exited with errors.
	pause
	exit /b 1
)

endlocal
