@echo off
cd /d %~dp0minecraft

set APPDATA=%~dp0minecraft
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot

java -Xmx2G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M -Djava.library.path=natives -cp "libraries/*;versions\1.21.4\1.21.4.jar;libraries\net\fabricmc\fabric-loader\0.19.2\fabric-loader-0.19.2.jar" net.fabricmc.loader.launch.knot.KnotClient --version fabric-loader-0.19.2-1.21.4 --accessToken 0 --gameDir . --assetsDir assets --assetIndex 1.21 --userType mojang --versionType release

pause