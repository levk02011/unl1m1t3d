import os
import re
import subprocess
import sys
import shutil
import urllib.request
from sys import argv, exit
from uuid import uuid1

# Імпорти PyQt5
from PyQt5.QtCore import QThread, pyqtSignal, QSize, Qt
from PyQt5.QtWidgets import (QWidget, QHBoxLayout, QVBoxLayout, QLabel, QLineEdit, 
                             QComboBox, QSpacerItem, QSizePolicy, QProgressBar, 
                             QPushButton, QApplication, QMainWindow, QMenuBar, 
                             QMenu, QAction, QMessageBox)
from PyQt5.QtGui import QPixmap, QIcon  # Додано QIcon для малювання кубика

# Minecraft Launcher Lib
from minecraft_launcher_lib.install import install_minecraft_version
from minecraft_launcher_lib.fabric import install_fabric
from minecraft_launcher_lib.command import get_minecraft_command

# Імпорти для генерації нікнеймів
from random_username.generate import generate_username

minecraft_directory = os.path.abspath(os.path.join(os.path.dirname(__file__), 'minecraft'))
os.makedirs(minecraft_directory, exist_ok=True)

class LaunchThread(QThread):
    launch_setup_signal = pyqtSignal(str, str)
    progress_update_signal = pyqtSignal(int, int, str)
    state_update_signal = pyqtSignal(bool)

    version_id = ''
    username = ''

    progress = 0
    progress_max = 0
    progress_label = ''

    def __init__(self):
        super().__init__()
        self.launch_setup_signal.connect(self.launch_setup)

    def launch_setup(self, version_id, username):
        self.version_id = version_id
        self.username = username
    
    def update_progress_label(self, value):
        self.progress_label = value
        self.progress_update_signal.emit(self.progress, self.progress_max, self.progress_label)
        
    def update_progress(self, value):
        self.progress = value
        self.progress_update_signal.emit(self.progress, self.progress_max, self.progress_label)
        
    def update_progress_max(self, value):
        self.progress_max = value
        self.progress_update_signal.emit(self.progress, self.progress_max, self.progress_label)

    def get_java_version(self, java_exe):
        try:
            result = subprocess.run([java_exe, '-version'], capture_output=True, text=True, timeout=5)
        except Exception:
            return None

        output = (result.stdout or '') + '\n' + (result.stderr or '')
        match = re.search(r'version \"([0-9._]+)\"', output)
        if not match:
            return None

        version_text = match.group(1)
        if version_text.startswith('1.'):
            parts = version_text.split('.')
            if len(parts) >= 2 and parts[1].isdigit():
                return int(parts[1])
            return None

        major = version_text.split('.')[0]
        return int(major) if major.isdigit() else None

    def find_java_executable(self):
        candidates = []
        for name in ['javaw', 'java']:
            path = shutil.which(name)
            if path:
                candidates.append(path)

        java_home = os.environ.get('JAVA_HOME')
        if java_home:
            for candidate in ['javaw.exe', 'java.exe']:
                candidate_path = os.path.join(java_home, 'bin', candidate)
                if os.path.exists(candidate_path):
                    candidates.append(candidate_path)

        program_files = os.environ.get('ProgramFiles', r'C:\Program Files')
        program_files_x86 = os.environ.get('ProgramFiles(x86)', r'C:\Program Files (x86)')
        for base in [program_files, program_files_x86]:
            java_root = os.path.join(base, 'Java')
            if os.path.isdir(java_root):
                for child in sorted(os.listdir(java_root), reverse=True):
                    for candidate in ['javaw.exe', 'java.exe']:
                        candidate_path = os.path.join(java_root, child, 'bin', candidate)
                        if os.path.exists(candidate_path):
                            candidates.append(candidate_path)

        unique_candidates = []
        for path in candidates:
            if path not in unique_candidates:
                unique_candidates.append(path)

        best_path = None
        best_version = -1
        for path in unique_candidates:
            version = self.get_java_version(path)
            if version is None:
                continue
            if version > best_version:
                best_version = version
                best_path = path

        return best_path

    def run(self):
        self.state_update_signal.emit(True)

        install_minecraft_version(version=self.version_id, minecraft_directory=minecraft_directory, callback={ 'setStatus': self.update_progress_label, 'setProgress': self.update_progress, 'setMax': self.update_progress_max })

        if self.version_id == '1.21.4':
            mods_dir = os.path.join(minecraft_directory, 'mods')
            os.makedirs(mods_dir, exist_ok=True)
            mod_jar_src = os.path.join(os.path.dirname(__file__), 'mod_1_21_4', 'build', 'libs', 'mod_1_21_4-1.0.0.jar')
            mod_jar_dst = os.path.join(mods_dir, 'mod_1_21_4-1.0.0.jar')
            if os.path.exists(mod_jar_src):
                if os.path.exists(mod_jar_dst):
                    os.remove(mod_jar_dst)
                shutil.copy2(mod_jar_src, mod_jar_dst)
                self.update_progress_label('Mod installed')
            else:
                self.update_progress_label('Mod JAR not found, build the mod first')

        if self.username == '':
            self.username = generate_username()[0]
        
        if self.version_id == '1.21.4':
            fabric_version = 'fabric-loader-0.19.2-1.21.4'
            self.update_progress_label('Installing Fabric version...')
            
            java_exe = self.find_java_executable()
            if java_exe is None:
                self.update_progress_label('Java not found. Install Java and add it to PATH or set JAVA_HOME.')
                self.state_update_signal.emit(False)
                return

            java_version = self.get_java_version(java_exe)
            if java_version is None or java_version < 17:
                self.update_progress_label('Java ' + (str(java_version) if java_version else 'unknown') + ' is too old. Install Java 17 or newer.')
                self.state_update_signal.emit(False)
                return

            try:
                install_fabric(minecraft_version=self.version_id, minecraft_directory=minecraft_directory, loader_version='0.19.2', callback={ 'setStatus': self.update_progress_label, 'setProgress': self.update_progress, 'setMax': self.update_progress_max }, java=java_exe)
            except Exception:
                install_minecraft_version(version=self.version_id, minecraft_directory=minecraft_directory, callback={ 'setStatus': self.update_progress_label, 'setProgress': self.update_progress, 'setMax': self.update_progress_max })
                self.update_progress_label('Fabric install failed, using manual Fabric launch fallback')
                self.launch_fabric_manually()
                return
            
            mods_dir = os.path.join(minecraft_directory, 'mods')
            os.makedirs(mods_dir, exist_ok=True)
            mod_jar_src = os.path.join(os.path.dirname(__file__), 'mod_1_21_4', 'build', 'libs', 'mod_1_21_4-1.0.0.jar')
            mod_jar_dst = os.path.join(mods_dir, 'mod_1_21_4-1.0.0.jar')
            if os.path.exists(mod_jar_src):
                if os.path.exists(mod_jar_dst):
                    os.remove(mod_jar_dst)
                shutil.copy2(mod_jar_src, mod_jar_dst)
                self.update_progress_label('Mod installed')
            else:
                self.update_progress_label('Mod JAR not found, build the mod first')

            fabric_api_url = 'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.115.1+1.21.4/fabric-api-0.115.1+1.21.4.jar'
            fabric_api_dst = os.path.join(mods_dir, 'fabric-api-0.115.1+1.21.4.jar')
            if not os.path.exists(fabric_api_dst):
                try:
                    urllib.request.urlretrieve(fabric_api_url, fabric_api_dst)
                    self.update_progress_label('Fabric API installed')
                except Exception as e:
                    self.update_progress_label(f'Failed to download Fabric API: {e}')

            if self.username == '':
                self.username = generate_username()[0]
            
            self.update_progress_label('Launching with Fabric...')
            
            java_exec = self.find_java_executable()
            if java_exec is None:
                self.update_progress_label('Java not found. Install Java and add it to PATH or set JAVA_HOME.')
                self.state_update_signal.emit(False)
                return
            java_version = self.get_java_version(java_exec)
            if java_version is None or java_version < 17:
                self.update_progress_label('Java ' + (str(java_version) if java_version else 'unknown') + ' is too old. Install Java 17 or newer.')
                self.state_update_signal.emit(False)
                return

            options = {
                'username': self.username,
                'uuid': str(uuid1()),
                'token': '',
                'launcherName': 'unl1m1t3d',
                'launcherVersion': '1.0',
                'executablePath': java_exec,
                'defaultExecutablePath': java_exec,
                'disableMultiplayer': False
            }

            command = get_minecraft_command(version=fabric_version, minecraft_directory=minecraft_directory, options=options)

            if os.name == 'nt' and command:
                javaw_path = None
                binary = os.path.basename(command[0]).lower()
                if binary == 'java.exe':
                    candidate = os.path.join(os.path.dirname(command[0]), 'javaw.exe')
                    if os.path.exists(candidate):
                        javaw_path = candidate
                if javaw_path:
                    command[0] = javaw_path

            creationflags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
            try:
                subprocess.Popen(command, cwd=minecraft_directory, creationflags=creationflags)
            except FileNotFoundError as e:
                self.update_progress_label(f'Launch failed: {e}')
                self.state_update_signal.emit(False)
                return
        else:
            java_runtime_path = None
            if self.version_id.startswith('1.21'):
                java_runtime_path = os.path.join(minecraft_directory, 'runtime', 'java-runtime-delta', 'bin', 'javaw.exe')
            else:
                java_runtime_path = os.path.join(minecraft_directory, 'runtime', 'jre-legacy', 'bin', 'javaw.exe')
            
            if not os.path.exists(java_runtime_path):
                java_runtime_path = self.find_java_executable()
                if java_runtime_path is None:
                    self.update_progress_label('Java executable not found. Install Java or set JAVA_HOME.')
                    self.state_update_signal.emit(False)
                    return
            
            options = {
                'username': self.username,
                'uuid': str(uuid1()),
                'token': '',
                'launcherName': 'unl1m1t3d',
                'launcherVersion': '1.0',
                'executablePath': java_runtime_path,
                'defaultExecutablePath': java_runtime_path,
                'disableMultiplayer': False
            }

            command = get_minecraft_command(version=self.version_id, minecraft_directory=minecraft_directory, options=options)

            if os.name == 'nt' and command:
                javaw_path = None
                binary = os.path.basename(command[0]).lower()
                if binary == 'java.exe':
                    candidate = os.path.join(os.path.dirname(command[0]), 'javaw.exe')
                    if os.path.exists(candidate):
                        javaw_path = candidate
                if javaw_path:
                    command[0] = javaw_path

            creationflags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
            subprocess.Popen(command, cwd=minecraft_directory, creationflags=creationflags)
        
        self.state_update_signal.emit(False)

    def launch_fabric_manually(self):
        if self.username == '':
            self.username = generate_username()[0]
        
        self.update_progress_label('Launching Fabric manually...')
        os.environ['APPDATA'] = minecraft_directory
        
        java_exe = self.find_java_executable()
        if java_exe is None:
            self.update_progress_label('Java executable not found. Install Java and add it to PATH or set JAVA_HOME.')
            self.state_update_signal.emit(False)
            return

        libraries_path = os.path.join(minecraft_directory, 'libraries')
        version_jar = os.path.join(minecraft_directory, 'versions', self.version_id, f'{self.version_id}.jar')
        fabric_loader_jar = os.path.join(libraries_path, 'net', 'fabricmc', 'fabric-loader', '0.19.2', 'fabric-loader-0.19.2.jar')
        
        classpath = os.pathsep.join([os.path.join(libraries_path, '*'), version_jar, fabric_loader_jar])
        
        command = [
            java_exe,
            '-Xmx2G',
            '-XX:+UnlockExperimentalVMOptions',
            '-XX:+UseG1GC',
            '-XX:G1NewSizePercent=20',
            '-XX:G1ReservePercent=20',
            '-XX:MaxGCPauseMillis=50',
            '-XX:G1HeapRegionSize=32M',
            '-Djava.library.path=natives',
            '-cp', classpath,
            'net.fabricmc.loader.launch.knot.KnotClient',
            '--version', f'fabric-loader-0.19.2-{self.version_id}',
            '--accessToken', '0',
            '--gameDir', '.',
            '--assetsDir', 'assets',
            '--assetIndex', '1.21',
            '--userType', 'mojang',
            '--versionType', 'release',
            '--username', self.username,
            '--uuid', str(uuid1())
        ]
        
        creationflags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
        try:
            subprocess.Popen(command, cwd=minecraft_directory, creationflags=creationflags)
        except FileNotFoundError:
            self.update_progress_label(f'Cannot launch Fabric: Java executable not found ({java_exe})')
        self.state_update_signal.emit(False)


class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()

        # Меню
        self.menubar = self.menuBar()
        self.about_menu = self.menubar.addMenu('About')
        self.about_us_action = QAction('About Us', self)
        self.about_us_action.triggered.connect(self.show_about_us)
        self.about_menu.addAction(self.about_us_action)

        self.resize(350, 283)
        self.setWindowTitle('Unl1m1t3d Launcher')
        self.centralwidget = QWidget(self)
        
        # Логотип
        self.logo = QLabel(self.centralwidget)
        self.logo.setMaximumSize(QSize(256, 37))
        self.logo.setText('')
        logo_path = 'assets/title.png'
        if os.path.exists(logo_path):
            self.logo.setPixmap(QPixmap(logo_path))
        else:
            self.logo.setText('Unl1m1t3d Launcher')
            self.logo.setStyleSheet('font-size: 18px; font-weight: bold;')
        self.logo.setScaledContents(True)
        
        self.titlespacer = QSpacerItem(20, 40, QSizePolicy.Policy.Minimum, QSizePolicy.Policy.Expanding)
        
        # --- ЮЗЕРНЕЙМ ТА КНОПКА ГЕНЕРАЦІЇ НІКНЕЙМУ (ЗАМІСТЬ ЧЕРВОНОГО КВАДРАТА) ---
        self.username = QLineEdit(self.centralwidget)
        self.username.setPlaceholderText('Username')
        
        # Створюємо горизонтальний контейнер, щоб поставити кнопку в один рядок із полем імені
        self.username_layout = QHBoxLayout()
        self.username_layout.addWidget(self.username)
        
        # Створення кнопки з кубиком
        self.random_username_button = QPushButton(self.centralwidget)
        dice_icon_path = 'assets/dice.png'
        
        if os.path.exists(dice_icon_path):
            self.random_username_button.setIcon(QIcon(dice_icon_path))
            self.random_username_button.setIconSize(QSize(20, 20))
            self.random_username_button.setFlat(True)  # Робимо її акуратною, без громіздких меж рамки
        else:
            # Текстовий варіант (емодзі), якщо іконку-файл не знайшли
            self.random_username_button.setText('🎲')
            self.random_username_button.setStyleSheet('font-size: 16px; border: 1px solid #bdc3c7; background: #ecf0f1; border-radius: 4px;')
            
        self.random_username_button.setFixedSize(30, 30)
        self.random_username_button.clicked.connect(self.generate_random_nickname)
        self.username_layout.addWidget(self.random_username_button)
        self.username_layout.setContentsMargins(0, 0, 0, 0)
        # ----------------------------------------------------------------------
        
        # Вибір версії
        self.version_select = QComboBox(self.centralwidget)
        self.version_select.addItem('1.21.4', '1.21.4')
        
        self.progress_spacer = QSpacerItem(20, 20, QSizePolicy.Policy.Minimum, QSizePolicy.Policy.Minimum)
        
        # Прогрес-бар та лейбл
        self.start_progress_label = QLabel(self.centralwidget)
        self.start_progress_label.setText('')
        self.start_progress_label.setVisible(False)

        self.start_progress = QProgressBar(self.centralwidget)
        self.start_progress.setProperty('value', 24)
        self.start_progress.setVisible(False)
        
        # Кнопка Play
        self.start_button = QPushButton(self.centralwidget)
        self.start_button.setText('Play')
        self.start_button.clicked.connect(self.launch_game)
        
        self.buttons_layout = QHBoxLayout()
        self.buttons_layout.addWidget(self.start_button)
        
        # Головна вертикальна розмітка вікна
        self.vertical_layout = QVBoxLayout(self.centralwidget)
        self.vertical_layout.setContentsMargins(15, 15, 15, 15)
        self.vertical_layout.addWidget(self.logo, 0, Qt.AlignmentFlag.AlignHCenter)
        self.vertical_layout.addItem(self.titlespacer)
        
        # Додаємо наш горизонтальний рядок (Поле введення + Кубик)
        self.vertical_layout.addLayout(self.username_layout)
        
        self.vertical_layout.addWidget(self.version_select)
        self.vertical_layout.addItem(self.progress_spacer)
        self.vertical_layout.addWidget(self.start_progress_label) 
        self.vertical_layout.addWidget(self.start_progress)
        self.vertical_layout.addLayout(self.buttons_layout)

        self.launch_thread = LaunchThread()
        self.launch_thread.state_update_signal.connect(self.state_update)
        self.launch_thread.progress_update_signal.connect(self.update_progress)

        self.setCentralWidget(self.centralwidget)
    
    def state_update(self, value):
        self.start_button.setDisabled(value)
        self.random_username_button.setDisabled(value)  # Блокуємо кубик під час запуску гри
        self.start_progress_label.setVisible(value)
        self.start_progress.setVisible(value)

    def update_progress(self, progress, max_progress, label):
        self.start_progress.setValue(progress)
        self.start_progress.setMaximum(max_progress)
        self.start_progress_label.setText(label) 
    
    def build_mod(self):
        try:
            mod_dir = os.path.join(os.path.dirname(__file__), 'mod_1_21_4')
            if not os.path.exists(mod_dir):
                QMessageBox.warning(self, 'Error', 'mod_1_21_4 directory not found!')
                return False
            
            gradlew = os.path.join(mod_dir, 'gradlew.bat')
            if not os.path.exists(gradlew):
                QMessageBox.warning(self, 'Error', 'gradlew.bat not found in mod_1_21_4!')
                return False
            
            self.update_progress_label('Building mod...')
            self.start_progress.setVisible(True)
            self.start_progress_label.setVisible(True)
            self.start_button.setEnabled(False)
            
            env = os.environ.copy()
            env['JAVA_HOME'] = r'C:\Program Files\Java\jdk-21.0.11'
            env['PATH'] = env.get('PATH', '') + ';' + r'C:\Program Files\Java\jdk-21.0.11\bin'
            
            creationflags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
            result = subprocess.run(
                [gradlew, 'build', '--stacktrace'],
                cwd=mod_dir,
                creationflags=creationflags,
                capture_output=True,
                text=True,
                env=env
            )
            
            if result.returncode != 0:
                QMessageBox.critical(self, 'Build Failed', f'Mod build failed:\n{result.stderr}')
                self.start_button.setEnabled(True)
                self.start_progress.setVisible(False)
                self.start_progress_label.setVisible(False)
                return False
            
            libs_dir = os.path.join(mod_dir, 'build', 'libs')
            jar_files = [f for f in os.listdir(libs_dir) if f.endswith('.jar') and 'mod_1_21_4' in f]
            
            if not jar_files:
                QMessageBox.warning(self, 'Error', 'No JAR file found in build/libs!')
                self.start_button.setEnabled(True)
                self.start_progress.setVisible(False)
                self.start_progress_label.setVisible(False)
                return False
            
            mods_dir = os.path.join(os.path.dirname(__file__), 'minecraft', 'mods')
            os.makedirs(mods_dir, exist_ok=True)
            
            src_jar = os.path.join(libs_dir, jar_files[0])
            dst_jar = os.path.join(mods_dir, jar_files[0])
            
            if os.path.exists(dst_jar):
                os.remove(dst_jar)
            shutil.copy2(src_jar, dst_jar)
            
            self.update_progress_label('Mod built and deployed!')
            return True
            
        except Exception as e:
            QMessageBox.critical(self, 'Error', f'Failed to build mod: {e}')
            self.start_button.setEnabled(True)
            self.start_progress.setVisible(False)
            self.start_progress_label.setVisible(False)
            return False
    
    def update_progress_label(self, text):
        self.start_progress_label.setText(text)
        QApplication.processEvents()
    
    # ФУНКЦІЯ ДЛЯ ГЕНЕРАЦІЇ НІКНЕЙМУ ПО КЛІКУ НА КУБИК
    def generate_random_nickname(self):
        generated = generate_username()[0]
        self.username.setText(generated)

    def launch_game(self):
        if not self.build_mod():
            return
        
        version_id = self.version_select.currentData() or self.version_select.currentText()
        
        # Перевірка: якщо користувач не ввів нікнейм і не тиснув на кубик, згенеруємо автоматично перед стартом
        current_username = self.username.text().strip()
        if not current_username:
            current_username = generate_username()[0]
            self.username.setText(current_username)

        self.launch_thread.launch_setup_signal.emit(version_id, current_username)
        self.launch_thread.start()

    def show_about_us(self):
        about_text = """
        <h2>Unl1m1t3d Launcher</h2>
        <p><b>Version:</b> 1.0</p>
        <p>A custom Minecraft launcher with Fabric mod support.</p>
        <p><b>Features:</b></p>
        <ul>
            <li>Easy Minecraft installation and launching</li>
            <li>Fabric mod loader support</li>
            <li>Automatic mod installation</li>
            <li>Progress tracking during installation</li>
        </ul>
        <p><b>Developed by:</b> Unl1m1t3d Team</p>
        <p>For more information, visit our website or check the project repository.</p>
        """
        QMessageBox.about(self, "About Unl1m1t3d Launcher", about_text)

    def launch_nether_wart_farm(self):
        QMessageBox.information(self, "NetherWartFarm", "NetherWartFarm function is under development.")

if __name__ == '__main__':
    QApplication.setAttribute(Qt.ApplicationAttribute.AA_EnableHighDpiScaling, True)

    app = QApplication(argv)
    window = MainWindow()
    window.show()

    exit(app.exec_())