import os
import minecraft_launcher_lib
import subprocess


def show_menu() -> str:
    print('=== Unl1m1t3d Launcher Menu ===')
    print('1. Install and launch Minecraft')
    print('2. Exit')
    return input('Choose an option: ').strip()


def main() -> None:
    minecraft_directory = os.path.abspath(os.path.join(os.path.dirname(__file__), 'minecraft'))
    os.makedirs(minecraft_directory, exist_ok=True)

    while True:
        choice = show_menu()
        if choice == '1':
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
            break
        elif choice == '2':
            print('Exit launcher.')
            break
        else:
            print('Invalid option. Please choose 1 or 2.')


if __name__ == '__main__':
    main()