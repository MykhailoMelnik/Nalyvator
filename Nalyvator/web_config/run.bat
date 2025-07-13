@echo off
echo 🌐 Компіляція Nalyvator Web Server...
javac NalyvatorWebServer.java
if %errorlevel% neq 0 (
    echo ❌ Помилка компіляції!
    pause
    exit /b 1
)
echo ✅ Компіляція успішна!
echo 🚀 Запуск сервера...
java NalyvatorWebServer 