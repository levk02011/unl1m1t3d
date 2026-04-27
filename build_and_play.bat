@echo off
REM Полный цикл: Компилирование -> Запуск игры
chcp 65001 > nul

echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║  Minecraft Mod - Build & Play                              ║
echo ║  Процес: Компіляція ^> Розгортання ^> Запуск гри             ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

cd /d %~dp0

REM Крок 1: Компіляція та розгортання
python build_and_deploy.py
if errorlevel 1 (
    echo.
    echo ❌ Компіляція не вдалась!
    pause
    exit /b 1
)

echo.
echo ✅ Компіляція завершена! Запускаю гру...
echo.

REM Крок 2: Запуск лаунчера з версією 1.21.4
timeout /t 2 /nobreak

REM Запускаємо лаунчер
python qt_version.py

pause
