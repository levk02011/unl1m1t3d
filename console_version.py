import os
import minecraft_launcher_lib
import subprocess

minecraft_directory = os.path.abspath(os.path.join(os.path.dirname(__file__), 'minecraft'))
os.makedirs(minecraft_directory, exist_ok=True)

version = input('Enter Minecraft version: ')
username = input('Enter Username: ')

# Install Minecraft version with all dependencies needed
minecraft_launcher_lib.install.install_minecraft_version(version=version, minecraft_directory=minecraft_directory)

# Define launch options
options = {
    'username': username,
    'uuid': '',
    'token': ''
}

# Launch Minecraft
subprocess.call(minecraft_launcher_lib.command.get_minecraft_command(version=version, minecraft_directory=minecraft_directory, options=options))