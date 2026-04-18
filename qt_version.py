import os

from PyQt5.QtCore import QThread, pyqtSignal, QSize, Qt
from PyQt5.QtWidgets import QWidget, QHBoxLayout, QVBoxLayout, QLabel, QLineEdit, QComboBox, QSpacerItem, QSizePolicy, QProgressBar, QPushButton, QApplication, QMainWindow
from PyQt5.QtGui import QPixmap

from minecraft_launcher_lib.install import install_minecraft_version
from minecraft_launcher_lib.command import get_minecraft_command

# Эти импорты не обязательны, вместо generate_username()[0] и str(uuid1()) можно оставить просто ''
from random_username.generate import generate_username
from uuid import uuid1

import subprocess
from sys import argv, exit
import shutil

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

import os

from PyQt5.QtCore import QThread, pyqtSignal, QSize, Qt
from PyQt5.QtWidgets import QWidget, QHBoxLayout, QVBoxLayout, QLabel, QLineEdit, QComboBox, QSpacerItem, QSizePolicy, QProgressBar, QPushButton, QApplication, QMainWindow
from PyQt5.QtGui import QPixmap

from minecraft_launcher_lib.install import install_minecraft_version
from minecraft_launcher_lib.command import get_minecraft_command

# Эти импорты не обязательны, вместо generate_username()[0] и str(uuid1()) можно оставить просто ''
from random_username.generate import generate_username
from uuid import uuid1

import subprocess
from sys import argv, exit
import shutil

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

    def run(self):
        self.state_update_signal.emit(True)

        install_minecraft_version(version=self.version_id, minecraft_directory=minecraft_directory, callback={ 'setStatus': self.update_progress_label, 'setProgress': self.update_progress, 'setMax': self.update_progress_max })

        # Check and install mod for 1.21.4
        if self.version_id == '1.21.4':
            mods_dir = os.path.join(minecraft_directory, 'mods')
            os.makedirs(mods_dir, exist_ok=True)
            mod_jar_src = os.path.join(os.path.dirname(__file__), 'mod_1_21_4', 'build', 'libs', 'mod_1_21_4-1.0.0.jar')
            mod_jar_dst = os.path.join(mods_dir, 'mod_1_21_4-1.0.0.jar')
            if not os.path.exists(mod_jar_dst):
                if os.path.exists(mod_jar_src):
                    shutil.copy2(mod_jar_src, mod_jar_dst)
                    self.update_progress_label('Mod installed')
                else:
                    self.update_progress_label('Mod JAR not found, build the mod first')

        if self.username == '':
            self.username = generate_username()[0]
        
        # For 1.21.4, use Fabric loader version
        if self.version_id == '1.21.4':
            fabric_version = 'fabric-loader-0.19.2-1.21.4'
            self.update_progress_label('Installing Fabric version...')
            
            # Try to install the Fabric version if it doesn't exist
            try:
                install_minecraft_version(version=fabric_version, minecraft_directory=minecraft_directory, callback={ 'setStatus': self.update_progress_label, 'setProgress': self.update_progress, 'setMax': self.update_progress_max })
            except:
                # If Fabric version install fails, install vanilla first
                install_minecraft_version(version=self.version_id, minecraft_directory=minecraft_directory, callback={ 'setStatus': self.update_progress_label, 'setProgress': self.update_progress, 'setMax': self.update_progress_max })
                self.update_progress_label('Fabric version not available, using vanilla with manual Fabric launch')
                # Fall back to manual Fabric launch
                self.launch_fabric_manually()
                return
            
            # Check and install mod for 1.21.4
            mods_dir = os.path.join(minecraft_directory, 'mods')
            os.makedirs(mods_dir, exist_ok=True)
            mod_jar_src = os.path.join(os.path.dirname(__file__), 'mod_1_21_4', 'build', 'libs', 'mod_1_21_4-1.0.0.jar')
            mod_jar_dst = os.path.join(mods_dir, 'mod_1_21_4-1.0.0.jar')
            if not os.path.exists(mod_jar_dst):
                if os.path.exists(mod_jar_src):
                    shutil.copy2(mod_jar_src, mod_jar_dst)
                    self.update_progress_label('Mod installed')
                else:
                    self.update_progress_label('Mod JAR not found, build the mod first')

            if self.username == '':
                self.username = generate_username()[0]
            
            self.update_progress_label('Launching with Fabric...')
            
            # Use the Fabric version command
            options = {
                'username': self.username,
                'uuid': str(uuid1()),
                'token': '',
                'launcherName': 'unl1m1t3d',
                'launcherVersion': '1.0',
                'executablePath': 'javaw',
                'defaultExecutablePath': 'javaw',
                'disableMultiplayer': False
            }

            command = get_minecraft_command(version=fabric_version, minecraft_directory=minecraft_directory, options=options)

            # Force javaw when possible
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
        else:
            # For other versions, use standard launcher
            # Determine Java runtime path based on version
            java_runtime_path = None
            if self.version_id.startswith('1.21'):
                # Use java-runtime-delta for modern versions
                java_runtime_path = os.path.join(minecraft_directory, 'runtime', 'java-runtime-delta', 'bin', 'javaw.exe')
            else:
                # Use jre-legacy for older versions like 1.16.5
                java_runtime_path = os.path.join(minecraft_directory, 'runtime', 'jre-legacy', 'bin', 'javaw.exe')
            
            # Fall back to default if runtime not found
            if not os.path.exists(java_runtime_path):
                java_runtime_path = 'javaw' 
            
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

            # Force javaw when possible so no extra JVM console appears, and use the runtime's javaw if available.
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
        """Fallback method to launch Fabric manually like fabric_start.bat"""
        if self.username == '':
            self.username = generate_username()[0]
        
        self.update_progress_label('Launching Fabric manually...')
        
        # Set environment variables like in fabric_start.bat
        os.environ['APPDATA'] = minecraft_directory
        
        # Find Java executable
        java_exe = 'java'  # Use system Java
        
        # Build classpath like fabric_start.bat
        libraries_path = os.path.join(minecraft_directory, 'libraries')
        version_jar = os.path.join(minecraft_directory, 'versions', self.version_id, f'{self.version_id}.jar')
        fabric_loader_jar = os.path.join(libraries_path, 'net', 'fabricmc', 'fabric-loader', '0.19.2', 'fabric-loader-0.19.2.jar')
        
        classpath = f'"{libraries_path}/*;{version_jar};{fabric_loader_jar}"'
        
        # Fabric launch command
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
        subprocess.Popen(command, cwd=minecraft_directory, creationflags=creationflags)
        self.state_update_signal.emit(False)

class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()

        self.resize(350, 283)
        self.setWindowTitle('Unl1m1t3d Launcher (оффлайн з модом)')
        self.centralwidget = QWidget(self)
        
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
        
        self.username = QLineEdit(self.centralwidget)
        self.username.setPlaceholderText('Username')
        
        self.version_select = QComboBox(self.centralwidget)
        self.version_select.addItem('1.21.4 (з модом Fabric)')
        
        self.progress_spacer = QSpacerItem(20, 20, QSizePolicy.Policy.Minimum, QSizePolicy.Policy.Minimum)
        
        # Исправил проблему с созданием описания для полосы прогресса
        # В [24:01] можно заметить что QProgressDialog работает не так как надо, а просто QProgressBar не имеет при себе описания
        # Из-за этого я удалил систему с описанием, но тут я ее добавил, просто дописав еще один элемент - QLabel, ну и не забыв добавить его в self.vertical_layout :)
        self.start_progress_label = QLabel(self.centralwidget)
        self.start_progress_label.setText('')
        self.start_progress_label.setVisible(False)

        self.start_progress = QProgressBar(self.centralwidget)
        self.start_progress.setProperty('value', 24)
        self.start_progress.setVisible(False)
        
        self.start_button = QPushButton(self.centralwidget)
        self.start_button.setText('Play')
        self.start_button.clicked.connect(self.launch_game)
        
        self.vertical_layout = QVBoxLayout(self.centralwidget)
        self.vertical_layout.setContentsMargins(15, 15, 15, 15)
        self.vertical_layout.addWidget(self.logo, 0, Qt.AlignmentFlag.AlignHCenter)
        self.vertical_layout.addItem(self.titlespacer)
        self.vertical_layout.addWidget(self.username)
        self.vertical_layout.addWidget(self.version_select)
        self.vertical_layout.addItem(self.progress_spacer)
        self.vertical_layout.addWidget(self.start_progress_label) # Исправил проблему с созданием описания для полосы прогресса [24:01]
        self.vertical_layout.addWidget(self.start_progress)
        self.vertical_layout.addWidget(self.start_button)

        self.launch_thread = LaunchThread()
        self.launch_thread.state_update_signal.connect(self.state_update)
        self.launch_thread.progress_update_signal.connect(self.update_progress)

        self.setCentralWidget(self.centralwidget)
    
    def state_update(self, value):
        self.start_button.setDisabled(value)
        self.start_progress_label.setVisible(value)
        self.start_progress.setVisible(value)
    def update_progress(self, progress, max_progress, label):
        self.start_progress.setValue(progress)
        self.start_progress.setMaximum(max_progress)
        self.start_progress_label.setText(label) # Исправил проблему с созданием описания для полосы прогресса [24:01]
    def launch_game(self):
        self.launch_thread.launch_setup_signal.emit(self.version_select.currentText(), self.username.text())
        self.launch_thread.start()

if __name__ == '__main__':
    QApplication.setAttribute(Qt.ApplicationAttribute.AA_EnableHighDpiScaling, True)

    app = QApplication(argv)
    window = MainWindow()
    window.show()

    exit(app.exec_())
