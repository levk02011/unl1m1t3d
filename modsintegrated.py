# modsintegrated.py
import os
import shutil
from pathlib import Path

def sync_mods():
    """
    Синхронізує моди з папки 'mods+' до папки модів гри.
    """
    print("🚀 Запуск синхронізації модів...")
    
    root = Path(__file__).parent.resolve()
    source_dir = root / "mods+"
    
    # Визначення цільової директорії для модів
    mods_dir = root / "minecraft" / "mods"

    if not mods_dir.exists():
        mods_dir = root / "mods"
        if not mods_dir.exists():
            mods_dir = root / "run" / "mods"

    print(f"📂 Папка-джерело: {source_dir}")
    print(f"📁 Папка призначення: {mods_dir}")

    if not source_dir.is_dir():
        print(f"❌ Помилка: Папка-джерело '{source_dir}' не знайдена.")
        return

    try:
        mods_dir.mkdir(parents=True, exist_ok=True)
    except OSError as e:
        print(f"❌ Помилка при створенні папки призначення: {e}")
        return

    try:
        mod_files = [f for f in source_dir.glob('*.jar')]
    except Exception as e:
        print(f"❌ Помилка при читанні папки-джерела: {e}")
        return

    if not mod_files:
        print("ℹ️ В папці 'mods+' немає .jar файлів для переміщення.")
        return

    print("\n🔄 Переміщення файлів:")
    for mod_file in mod_files:
        try:
            destination_path = mods_dir / mod_file.name
            shutil.move(str(mod_file), str(destination_path))
            print(f"  ✅ {mod_file.name} -> {destination_path}")
        except shutil.Error as e:
             if "already exists" in str(e):
                 print(f"  ⚠️  Файл {mod_file.name} вже існує в папці призначення. Видаляємо оригінал.")
                 try:
                     os.remove(mod_file)
                 except OSError as remove_error:
                     print(f"  ❌ Не вдалося видалити оригінальний файл {mod_file.name}: {remove_error}")
             else:
                 print(f"  ❌ Помилка переміщення {mod_file.name}: {e}")
        except Exception as e:
            print(f"  ❌ Невідома помилка при переміщенні {mod_file.name}: {e}")
    
    print("\n✅ Синхронізацію модів завершено.")

if __name__ == "__main__":
    sync_mods()
