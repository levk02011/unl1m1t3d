# Тестування Minecraft мода

## Що зроблено:
- Мод скомпільовано успішно: `mod_1_21_4/build/libs/mod_1_21_4-1.0.0.jar`
- Встановлено Prism Launcher для тестування модів
- Створено папку `minecraft/mods/` і скопійовано туди JAR файл мода

## Як протестувати мод:

1. Запустіть `start_prism.bat` - це відкриє Prism Launcher
2. В Prism Launcher:
   - Натисніть "Add Instance"
   - Виберіть "Minecraft" -> "Release" -> "1.21.4"
   - Натисніть "OK" щоб створити інстанс
3. Для створеного інстансу:
   - Клікніть правою кнопкою -> "Edit Instance"
   - Перейдіть в "Mods" -> "Download mods"
   - Знайдіть і встановіть Fabric API
   - Або вручну скопіюйте `mod_1_21_4-1.0.0.jar` в папку mods інстансу
4. Запустіть гру через "Launch" в Prism Launcher

## Альтернативний спосіб:
Якщо у вас встановлений офіційний Minecraft Launcher:
1. Відкрийте launcher
2. Перейдіть в "Installations" -> "New Installation"
3. Виберіть версію 1.21.4
4. Змініть назву на "Fabric 1.21.4"
5. В "Game Directory" вкажіть шлях до папки `minecraft` в цьому проекті
6. Встановіть Fabric через fabricmc.net/install
7. Скопіюйте мод в `minecraft/mods/`
8. Запустіть гру

Мод готовий до тестування!