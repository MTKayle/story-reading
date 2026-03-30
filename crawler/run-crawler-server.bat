@echo off
setlocal

REM Move to repo root (same dir as this script)
cd /d "%~dp0"

REM Ensure Node project exists
if not exist "web\package.json" (
	echo [ERROR] Thu muc web hoac package.json khong ton tai.
	pause
	exit /b 1
)

cd web

REM Install deps if node_modules missing
if not exist "node_modules" (
	echo [INFO] Chua co node_modules, dang chay npm install...
	cmd /c npm install
	if errorlevel 1 (
		echo [ERROR] npm install that bai.
		pause
		exit /b 1
	)
)

echo.
echo [INFO] Khoi dong crawler server (npm run crawler)...
echo.
cmd /c npm run crawler

echo.
echo [INFO] Crawler server da dung.
pause
endlocal

