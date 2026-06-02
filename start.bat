@echo off
cd /d %~dp0

REM Встановлюємо правильний JAVA_HOME
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [*] JAVA_HOME встановлено: %JAVA_HOME%
echo [*] Перевіряю JDK...
java -version
echo.

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