@echo off
chcp 65001 >nul
echo 🔧 Автоматичне встановлення Java та Arduino CLI
echo ===============================================

echo 🔍 Перевірка Java...
java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Java вже встановлена
    goto check_arduino
)

echo ❌ Java не встановлена
echo 📥 Завантаження Java...
echo.

REM Створюємо тимчасову папку
if not exist "%TEMP%\java_install" mkdir "%TEMP%\java_install"
cd /d "%TEMP%\java_install"

REM Завантажуємо Java (OpenJDK 17)
echo 🔄 Завантаження OpenJDK 17...
powershell -Command "& {Invoke-WebRequest -Uri 'https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip' -OutFile 'openjdk.zip'}"

if not exist "openjdk.zip" (
    echo ❌ Помилка завантаження Java
    echo 🔗 Встановіть Java вручну: https://java.com/download/
    pause
    exit /b 1
)

echo 📦 Розпакування Java...
powershell -Command "& {Expand-Archive -Path 'openjdk.zip' -DestinationPath '.' -Force}"

echo 🔧 Встановлення Java...
for /d %%i in (jdk-*) do (
    set "JAVA_HOME=%%i"
    setx JAVA_HOME "%%i" /M
    setx PATH "%PATH%;%%i\bin" /M
)

echo ✅ Java встановлена!

:check_arduino
echo.
echo 🔍 Перевірка Arduino CLI...
arduino-cli version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Arduino CLI вже встановлений
    goto install_libraries
)

echo ❌ Arduino CLI не встановлений
echo 📥 Встановлення Arduino CLI...

REM Завантажуємо Arduino CLI
echo 🔄 Завантаження Arduino CLI...
powershell -Command "& {Invoke-WebRequest -Uri 'https://downloads.arduino.cc/arduino-cli/arduino-cli_latest_Windows_64bit.zip' -OutFile 'arduino-cli.zip'}"

if not exist "arduino-cli.zip" (
    echo ❌ Помилка завантаження Arduino CLI
    echo 🔗 Встановіть Arduino CLI вручну: https://arduino.github.io/arduino-cli/latest/installation/
    pause
    exit /b 1
)

echo 📦 Розпакування Arduino CLI...
powershell -Command "& {Expand-Archive -Path 'arduino-cli.zip' -DestinationPath '.' -Force}"

echo 🔧 Встановлення Arduino CLI...
copy "arduino-cli.exe" "%USERPROFILE%\AppData\Local\Microsoft\WinGet\Packages" >nul 2>&1
setx PATH "%PATH%;%USERPROFILE%\AppData\Local\Microsoft\WinGet\Packages" /M

echo ✅ Arduino CLI встановлений!

:install_libraries
echo.
echo 📚 Встановлення Arduino бібліотек...
echo.

echo 🔧 Встановлення Servo...
arduino-cli lib install Servo
if %errorlevel% neq 0 (
    echo ❌ Помилка встановлення Servo
) else (
    echo ✅ Servo встановлено
)

echo 🔧 Встановлення GyverTM1637...
arduino-cli lib install GyverTM1637
if %errorlevel% neq 0 (
    echo ❌ Помилка встановлення GyverTM1637
) else (
    echo ✅ GyverTM1637 встановлено
)

echo 🔧 Встановлення ServoSmooth...
arduino-cli lib install ServoSmooth
if %errorlevel% neq 0 (
    echo ❌ Помилка встановлення ServoSmooth
) else (
    echo ✅ ServoSmooth встановлено
)

echo.
echo 🧹 Очищення тимчасових файлів...
cd /d "%~dp0"
rmdir /s /q "%TEMP%\java_install" >nul 2>&1

echo.
echo ✅ Всі компоненти встановлено!
echo 🚀 Тепер можна запускати run.bat
echo.
pause 