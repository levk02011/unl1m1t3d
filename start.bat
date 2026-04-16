@echo off
cd /d %~dp0
if exist .venv\Scripts\pythonw.exe (
    start "" /B .venv\Scripts\pythonw.exe qt_version.py
) else (
    start "" /B py -3w qt_version.py
)
exit /b 0
