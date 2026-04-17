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

        if self.username == '':
            self.username = generate_username()[0]
        
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

class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()

        self.resize(300, 283)
        self.setWindowTitle('Minecraft Launcher')
        self.centralwidget = QWidget(self)
        
        self.logo = QLabel(self.centralwidget)
        self.logo.setMaximumSize(QSize(256, 37))
        self.logo.setText('')
        self.logo.setPixmap(QPixmap('assets/title.png'))
        self.logo.setScaledContents(True)
        
        self.titlespacer = QSpacerItem(20, 40, QSizePolicy.Policy.Minimum, QSizePolicy.Policy.Expanding)
        
        self.username = QLineEdit(self.centralwidget)
        self.username.setPlaceholderText('Username')
        
        self.version_select = QComboBox(self.centralwidget)
        for version in ['1.21.4']:
            self.version_select.addItem(version)
        
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
#DESKTOP-SO5QVIC