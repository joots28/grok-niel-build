@echo off
setlocal
set GRADLE_VERSION=8.13
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
set "BASE=%USERPROFILE%\.gradle\manual"
set "INSTALL_DIR=%BASE%\gradle-%GRADLE_VERSION%"
if not exist "%INSTALL_DIR%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%BASE%\gradle-%GRADLE_VERSION%-bin.zip'; Expand-Archive -Path '%BASE%\gradle-%GRADLE_VERSION%-bin.zip' -DestinationPath '%BASE%' -Force; Remove-Item '%BASE%\gradle-%GRADLE_VERSION%-bin.zip'"
  if not exist "%INSTALL_DIR%\bin\gradle.bat" exit /b 1
)
call "%INSTALL_DIR%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
