@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM Usage:
REM   ci_check.cmd       -> loads .env.shared + .env.ci (default)
REM   ci_check.cmd ci    -> same as default
REM
REM Runs a CI-parity backend check against a hosted Supabase database:
REM 1) hard-reset public schema
REM 2) apply pending migrations
REM 3) run maven clean verify with CI-style env vars

set "MODE=%~1"
if "%MODE%"=="" set "MODE=ci"

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

call :require_env CI_DB_URL_JDBC || exit /b 1
call :require_env CI_DB_USER || exit /b 1
call :require_env CI_DB_PASSWORD || exit /b 1
call :require_env CI_SUPABASE_JWKS_URL || exit /b 1

where npx >nul 2>nul
if errorlevel 1 (
  echo Error: npx is required to run Supabase CLI
  exit /b 1
)

where powershell >nul 2>nul
if errorlevel 1 (
  echo Error: powershell is required to URL-encode CI_DB_PASSWORD.
  exit /b 1
)

where psql >nul 2>nul
if errorlevel 1 (
  echo Error: psql is required to reset hosted CI DB schema.
  exit /b 1
)

set "JDBC_PREFIX=jdbc:postgresql://"
if /I not "%CI_DB_URL_JDBC:~0,18%"=="%JDBC_PREFIX%" (
  echo Error: CI_DB_URL_JDBC must start with %JDBC_PREFIX%
  exit /b 1
)

for /f "usebackq delims=" %%I in (`powershell -NoProfile -Command "[uri]::EscapeDataString($env:CI_DB_PASSWORD)"`) do set "ENCODED_DB_PASSWORD=%%I"
if "%ENCODED_DB_PASSWORD%"=="" (
  echo Error: failed to URL-encode CI_DB_PASSWORD.
  exit /b 1
)

set "JDBC_SUFFIX=%CI_DB_URL_JDBC:~18%"
set "PG_URL=postgresql://%CI_DB_USER%:%ENCODED_DB_PASSWORD%@%JDBC_SUFFIX%"

echo Resetting hosted CI DB public schema...
psql "%PG_URL%" -v ON_ERROR_STOP=1 -c "drop schema if exists public cascade;"
if errorlevel 1 exit /b 1
psql "%PG_URL%" -v ON_ERROR_STOP=1 -c "create schema public;"
if errorlevel 1 exit /b 1
psql "%PG_URL%" -v ON_ERROR_STOP=1 -c "grant usage on schema public to postgres, anon, authenticated, service_role;"
if errorlevel 1 exit /b 1
psql "%PG_URL%" -v ON_ERROR_STOP=1 -c "grant all on schema public to postgres, service_role;"
if errorlevel 1 exit /b 1
psql "%PG_URL%" -v ON_ERROR_STOP=1 -c "grant all on schema public to anon, authenticated;"
if errorlevel 1 exit /b 1
psql "%PG_URL%" -v ON_ERROR_STOP=1 -c "do $$ begin if exists (select 1 from information_schema.tables where table_schema = 'supabase_migrations' and table_name = 'schema_migrations') then delete from supabase_migrations.schema_migrations; end if; end $$;"
if errorlevel 1 exit /b 1

echo Applying pending Supabase migrations to hosted CI DB...
npx supabase migration up --db-url "%PG_URL%"
if errorlevel 1 exit /b 1

echo Running backend CI verify ^(mvn clean verify^)...
set "SPRING_PROFILES_ACTIVE=ci"
set "CI_DB_URL_JDBC=%CI_DB_URL_JDBC%"
set "CI_DB_USER=%CI_DB_USER%"
set "CI_DB_PASSWORD=%CI_DB_PASSWORD%"
set "CI_SUPABASE_JWKS_URL=%CI_SUPABASE_JWKS_URL%"

call "%SCRIPT_DIR%mvnw.cmd" -B clean verify
exit /b %errorlevel%

:load_env
set "FILE=%~1"
for /f "usebackq tokens=1* delims==" %%K in (`findstr /r /v "^[ ]*# ^[ ]*$" "%FILE%"`) do (
  if not "%%K"=="" set "%%K=%%L"
)
exit /b 0

:require_env
set "KEY=%~1"
if "%KEY%"=="" exit /b 1
if not defined %KEY% (
  echo Error: %KEY% is required.
  exit /b 1
)
exit /b 0
