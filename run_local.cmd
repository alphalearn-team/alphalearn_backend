@echo off
setlocal EnableExtensions

REM Usage:
REM   run_local.cmd            -> loads .env.shared + .env.local (default)
REM   run_local.cmd production -> loads .env.shared + .env.production

set "MODE=%~1"
if "%MODE%"=="" set "MODE=local"

set "SCRIPT_DIR=%~dp0"
set "ENV_SHARED=%SCRIPT_DIR%.env.shared"
set "ENV_MODE=%SCRIPT_DIR%.env.%MODE%"

if not exist "%ENV_SHARED%" (
  echo Error: %ENV_SHARED% not found. Create it with shared settings.
  exit /b 1
)

if not exist "%ENV_MODE%" (
  echo Error: %ENV_MODE% not found. Create it for '%MODE%' mode.
  exit /b 1
)

echo Loading env files: %ENV_SHARED% + %ENV_MODE%
call :load_env "%ENV_SHARED%" || exit /b 1
call :load_env "%ENV_MODE%" || exit /b 1

call "%SCRIPT_DIR%mvnw.cmd" spring-boot:run
exit /b %errorlevel%

:load_env
set "FILE=%~1"
for /f "usebackq tokens=1* delims==" %%K in (`findstr /r /v "^[ ]*# ^[ ]*$" "%FILE%"`) do (
  if not "%%K"=="" set "%%K=%%L"
)
exit /b 0
