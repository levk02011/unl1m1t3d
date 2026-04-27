@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM Переходимо в папку скрипту
cd /d %~dp0

REM Встановлюємо JAVA_HOME для Gradle
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot

REM Запускаємо Python скрипт
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║   Minecraft Mod - Build & Deploy Launcher                  ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

python build_and_deploy.py

if errorlevel 1 (
    echo.
    echo ❌ Помилка! Перевірте консоль вище.
    pause
    exit /b 1
)

echo.
pause
