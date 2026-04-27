#!/usr/bin/env python3
"""
Автоматизований скрипт для компіляції та розгортання Minecraft моду
Компілює мод через Gradle і копіює готовий JAR у папку модів
"""

import os
import sys
import shutil
import subprocess
from pathlib import Path

def main():
    # Визначаємо шляхи
    root_dir = Path(__file__).parent
    mod_dir = root_dir / 'mod_1_21_4'
    build_dir = mod_dir / 'build' / 'libs'
    minecraft_dir = root_dir / 'minecraft'
    mods_dir = minecraft_dir / 'mods'
    
    print("=" * 60)
    print("🔨 Minecraft Mod - Build & Deploy")
    print("=" * 60)
    
    # Перевірка наявності мод-проекту
    if not mod_dir.exists():
        print(f"❌ Папка мода не знайдена: {mod_dir}")
        sys.exit(1)
    
    # Крок 1: Очистка попередніх збірок
    print("\n📦 Крок 1: Підготовка...")
    if build_dir.exists():
        print(f"  Видаляю старі збірки: {build_dir}")
        shutil.rmtree(build_dir)
    
    # Крок 2: Компіляція
    print("\n🔨 Крок 2: Компіляція моду...")
    try:
        result = subprocess.run(
            ['gradlew.bat', 'clean', 'build', '--no-daemon'],
            cwd=str(mod_dir),
            capture_output=False,
            text=True
        )
        
        if result.returncode != 0:
            print("❌ Помилка при компіляції!")
            sys.exit(1)
        
        print("✅ Компіляція успішна!")
    except Exception as e:
        print(f"❌ Помилка запуску Gradle: {e}")
        sys.exit(1)
    
    # Крок 3: Пошук готового JAR
    print("\n🔍 Крок 3: Пошук готового JAR...")
    jar_files = list(build_dir.glob('mod_1_21_4-*.jar'))
    if not jar_files:
        print(f"❌ JAR файл не знайдений в {build_dir}")
        sys.exit(1)
    
    # Беремо останній створений JAR
    jar_file = sorted(jar_files)[-1]
    print(f"  Знайдений JAR: {jar_file.name}")
    
    # Крок 4: Розгортання (копіювання)
    print("\n📂 Крок 4: Розгортання...")
    os.makedirs(mods_dir, exist_ok=True)
    
    dest_jar = mods_dir / jar_file.name
    print(f"  Копіюю в: {mods_dir}")
    
    # Видаляємо старою версію, якщо існує
    if dest_jar.exists():
        print(f"  Видаляю стару версію: {dest_jar.name}")
        os.remove(dest_jar)
    
    try:
        shutil.copy2(jar_file, dest_jar)
        print(f"✅ Мод успішно розгорнуто: {dest_jar.name}")
    except Exception as e:
        print(f"❌ Помилка при копіюванні: {e}")
        sys.exit(1)
    
    # Крок 5: Завершення
    print("\n" + "=" * 60)
    print("✅ ГОТОВО! Мод готовий до запуску в Minecraft")
    print("=" * 60)
    print(f"\n📍 Шлях до моду: {dest_jar}")
    print(f"🎮 Запустіть fabric_start.bat для початку гри\n")
    
    input("Натисніть Enter для завершення...")

if __name__ == '__main__':
    main()
