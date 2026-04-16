@echo off
cd /d %~dp0
if exist .venv\Scripts\pythonw.exe (
    start "" /B .venv\Scripts\pythonw.exe qt_version.py
) else if exist .venv\Scripts\python.exe (
    start "" /B .venv\Scripts\python.exe qt_version.pyw
) else (
    start "" /B pyw -3 qt_version.pyw
)
exit /b 0
