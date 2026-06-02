@echo off
cd /d %~dp0

REM Спочатку запускаємо перевірку залежностей
call startcheck.bat
if errorlevel 1 (
    echo [!] startcheck.bat завершився з помилкою
    pause
    exit /b 1
)

echo.
echo [*] Роботу програми завершено.
pause
exit /b 0