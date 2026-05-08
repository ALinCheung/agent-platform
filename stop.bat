@echo off
REM Stop process using port 8080 (Windows)

set FOUND=0
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo Stopping process PID: %%a
    taskkill /F /PID %%a
    set FOUND=1
)

if %FOUND%==0 (
    echo No process found listening on port 8080
)
