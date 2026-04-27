#!/usr/bin/env python3
"""
Швидкий тест компіляції
"""

import os
import sys
from pathlib import Path

def main():
    print("\n" + "="*60)
    print("✅ Проверка системи...")
    print("="*60)
    
    root = Path(__file__).parent
    
    # Проверка Python
    print(f"\n🐍 Python: {sys.version.split()[0]}")
    
    # Проверка структури
    checks = {
        "mod_1_21_4": root / "mod_1_21_4",
        "minecraft": root / "minecraft",
        "build_and_deploy.py": root / "build_and_deploy.py",
        "qt_version.py": root / "qt_version.py",
        "fabric_start.bat": root / "fabric_start.bat",
    }
    
    print("\n📂 Структура проекту:")
    for name, path in checks.items():
        status = "✅" if path.exists() else "❌"
        print(f"  {status} {name}")
    
    # Проверка JAR
    print("\n📦 Статус модулів:")
    jar_src = root / "mod_1_21_4" / "build" / "libs" / "mod_1_21_4-1.0.0.jar"
    jar_dst = root / "minecraft" / "mods" / "mod_1_21_4-1.0.0.jar"
    
    if jar_src.exists():
        size = jar_src.stat().st_size / 1024
        print(f"  ✅ Build: mod_1_21_4-1.0.0.jar ({size:.1f} KB)")
    else:
        print(f"  ❌ Build: mod_1_21_4-1.0.0.jar (НЕ ЗНАЙДЕНО)")
    
    if jar_dst.exists():
        size = jar_dst.stat().st_size / 1024
        print(f"  ✅ Mods folder: mod_1_21_4-1.0.0.jar ({size:.1f} KB)")
    else:
        print(f"  ❌ Mods folder: mod_1_21_4-1.0.0.jar (НЕ ЗНАЙДЕНО)")
    
    # Рекомендації
    print("\n" + "="*60)
    print("🚀 НАСТУПНІ КРОКИ:")
    print("="*60)
    
    if jar_dst.exists():
        print("\n✅ Мод готовий до запуску! Вибіріть один варіант:")
        print("\n  1️⃣  Запустити через батник:")
        print("      • build_mod.bat        - Компіляція + розгортання")
        print("      • build_and_play.bat   - Компіляція + гра")
        print("      • fabric_start.bat     - Тільки запуск гри")
        print("\n  2️⃣  Запустити через GUI:")
        print("      • python qt_version.py - Лаунчер з кнопкою Build Mod")
        print("\n  3️⃣  Компіляція скриптом:")
        print("      • python build_and_deploy.py")
    else:
        print("\n❌ Мод не готовий. Запустіть компіляцію:")
        print("   • build_mod.bat")
        print("   • або python build_and_deploy.py")
    
    print("\n" + "="*60 + "\n")

if __name__ == '__main__':
    main()
