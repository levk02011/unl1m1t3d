@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

echo [*] Перевірка необхідних компонентів...
echo.

set JAVA_FOUND=0
set PYTHON_FOUND=0
set DOWNLOAD_DIR=%~dp0download+

REM Перевірка JDK
echo [*] Перевіряю JDK 21+...
java -version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    for /f "tokens=2" %%i in ('java -version 2^>^&1 ^| findstr "version"') do (
        set "JAVA_VERSION=%%i"
        set "JAVA_VERSION=!JAVA_VERSION:"=!"
        for /f "tokens=1 delims=." %%a in ("!JAVA_VERSION!") do (
            set "JAVA_MAJOR=%%a"
        )
        if !JAVA_MAJOR! geq 21 (
            set JAVA_FOUND=1
            echo [✓] JDK знайдена: версія !JAVA_VERSION!
        )
    )
)

if !JAVA_FOUND! equ 0 (
    echo [✗] JDK 21+ не знайдена
)

REM Перевірка Python
echo [*] Перевіряю Python 3.x...
python --version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    for /f "tokens=2" %%i in ('python --version 2^>^&1') do (
        set "PYTHON_VERSION=%%i"
        set "PYTHON_VERSION=!PYTHON_VERSION:"=!"
        for /f "tokens=1 delims=." %%a in ("!PYTHON_VERSION!") do (
            set "PYTHON_MAJOR=%%a"
        )
        if !PYTHON_MAJOR! equ 3 (
            set PYTHON_FOUND=1
            echo [✓] Python знайдена: версія !PYTHON_VERSION!
        )
    )
)

if !PYTHON_FOUND! equ 0 (
    echo [✗] Python 3.x не знайдена в системному PATH
)

echo.
echo ===============================================
echo [*] Результат первинної перевірки:
echo    JDK 21+: %JAVA_FOUND%
echo    Python 3.x: %PYTHON_FOUND%
echo ===============================================
echo.

REM Якщо чогось немає - качаємо та ставимо
if !JAVA_FOUND! equ 0 call :download_jdk
if !PYTHON_FOUND! equ 0 call :download_python

echo.
echo [*] Налаштування середовища...

REM Тимчасово оновлюємо PATH для поточної сесії
set "PATH=%PATH%;C:\Program Files\Java\jdk-21.0.11\bin;C:\Program Files\Java\jdk-21\bin;C:\Program Files\Python312\;C:\Program Files\Python312\Scripts\;%USERPROFILE%\AppData\Local\Programs\Python\Python312\;%USERPROFILE%\AppData\Local\Programs\Python\Python312\Scripts\"
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"

echo [*] JAVA_HOME встановлено: %JAVA_HOME%
echo [*] PATH оновлено

REM Визначаємо точний шлях до робочого файлу python.exe
set "FINAL_PYTHON="

if exist ".venv\Scripts\python.exe" (
    set "FINAL_PYTHON=.venv\Scripts\python.exe"
    echo [✓] Використовується віртуальне оточення .venv
) else (
    python --version >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        set "FINAL_PYTHON=python"
        echo [✓] Використовується системний Python з PATH
    ) else if exist "C:\Program Files\Python312\python.exe" (
        set "FINAL_PYTHON=C:\Program Files\Python312\python.exe"
        echo [✓] Знайдено Python у Program Files
    ) else if exist "%USERPROFILE%\AppData\Local\Programs\Python\Python312\python.exe" (
        set "FINAL_PYTHON=%USERPROFILE%\AppData\Local\Programs\Python\Python312\python.exe"
        echo [✓] Знайдено Python у AppData Local
    )
)

if "%FINAL_PYTHON%"=="" (
    echo [✗] Критична помилка: Не вдалося знайти інсталяцію Python.
    echo     Будь ласка, запустіть інсталятор з папки download+ вручну.
    pause
    exit /b 1
)

echo.
echo [*] Перевірка та автоматичне встановлення бібліотек...
echo     (Будь ласка, зачекайте, це може зайняти хвилину)...
echo.

REM Перевірка та встановлення PyQt5
"%FINAL_PYTHON%" -c "import PyQt5" >nul 2>&1
if errorlevel 1 (
    echo [*] Встановлюю PyQt5...
    "%FINAL_PYTHON%" -m pip install PyQt5 --quiet
    if errorlevel 1 (echo [✗] Не вдалося встановити PyQt5) else (echo [✓] PyQt5 успішно встановлено)
) else (
    echo [✓] PyQt5 вже встановлено.
)

REM Перевірка та встановлення minecraft_launcher_lib
"%FINAL_PYTHON%" -c "import minecraft_launcher_lib" >nul 2>&1
if errorlevel 1 (
    echo [*] Встановлюю minecraft_launcher_lib...
    "%FINAL_PYTHON%" -m pip install minecraft-launcher-lib --quiet
    if errorlevel 1 (echo [✗] Не вдалося встановити minecraft_launcher_lib) else (echo [✓] minecraft_launcher_lib успішно встановлено)
) else (
    echo [✓] minecraft_launcher_lib вже встановлено.
)

REM Перевірка та встановлення random_username
"%FINAL_PYTHON%" -c "import random_username" >nul 2>&1
if errorlevel 1 (
    echo [*] Встановлюю random_username...
    "%FINAL_PYTHON%" -m pip install random-username --quiet
    if errorlevel 1 (echo [✗] Не вдалося встановити random_username) else (echo [✓] random_username успішно встановлено)
) else (
    echo [✓] random_username вже встановлено.
)

echo.
echo [*] Пошук та запуск лаунчера...
cd /d %~dp0
if exist qt_version.py (
    echo [✓] Файл qt_version.py знайдено. Запуск додатка...
    echo.
    
    if "%FINAL_PYTHON%"=="python" (
        if exist .venv\Scripts\pythonw.exe (
            call .venv\Scripts\pythonw.exe qt_version.py
        ) else (
            call python qt_version.py
        )
    ) else (
        call "%FINAL_PYTHON%" qt_version.py
    )
    exit /b 0
) else (
    echo [✗] Помилка: Файл qt_version.py не знайдено в %CD%
    pause
    exit /b 1
)

exit /b 0

:download_jdk
if not exist "!DOWNLOAD_DIR!" mkdir "!DOWNLOAD_DIR!"
echo [*] Завантажую JDK 21...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "$url = 'https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.exe'; " ^
    "$output = '!DOWNLOAD_DIR!\jdk-21-installer.exe'; " ^
    "try { " ^
    "  Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing; " ^
    "  Write-Host '[✓] JDK 21 завантажено'; " ^
    "  Write-Host '[*] Запуск установника Java (тиха інсталяція)...'; " ^
    "  $installArgs = '/s INSTALLDIR=C:\Program Files\Java\jdk-21'; " ^
    "  Start-Process -FilePath $output -ArgumentList $installArgs -Wait; " ^
    "  Write-Host '[✓] JDK 21 встановлена'; " ^
    "  Write-Host '[*] Встановлюю JAVA_HOME у системну змінну оточення...'; " ^
    "  [Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-21', 'Machine'); " ^
    "  [Environment]::SetEnvironmentVariable('PATH', [Environment]::GetEnvironmentVariable('PATH', 'Machine') + ';C:\Program Files\Java\jdk-21\bin', 'Machine'); " ^
    "  Write-Host '[✓] JAVA_HOME та PATH встановлені'; " ^
    "} catch { " ^
    "  Write-Host '[✗] Помилка завантаження JDK: ' + $_.Exception.Message; " ^
    "}"
exit /b 0

:download_python
if not exist "!DOWNLOAD_DIR!" mkdir "!DOWNLOAD_DIR!"
echo [*] Завантажую Python 3.12...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "$url = 'https://www.python.org/ftp/python/3.12.0/python-3.12.0-amd64.exe'; " ^
    "$output = '!DOWNLOAD_DIR!\python-3.12-installer.exe'; " ^
    "try { " ^
    "  Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing; " ^
    "  Write-Host '[✓] Python 3.12 завантажено'; " ^
    "  Write-Host '[*] Запуск установника Python (тиха інсталяція)...'; " ^
    "  Start-Process -FilePath $output -ArgumentList '/quiet InstallAllUsers=1 PrependPath=1 Include_test=0' -Wait; " ^
    "  Write-Host '[✓] Python 3.12 встановлена'; " ^
    "} catch { " ^
    "  Write-Host '[✗] Помилка завантаження Python: ' + $_.Exception.Message; " ^
    "}"
exit /b 0