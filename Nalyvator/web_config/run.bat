@echo off
chcp 65001 >nul
echo 🌐 Nalyvator Config - Windows
echo =============================

echo 🔍 Перевірка Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java не встановлена!
    echo Встановіть Java з https://java.com/download/
    pause
    exit /b 1
)
echo ✅ Java знайдена

echo 🔍 Перевірка порту 8081...
netstat -an | findstr :8081 >nul
if %errorlevel% equ 0 (
    echo 🛑 Порт 8081 зайнятий, зупиняємо процеси...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do (
        taskkill /PID %%a /F >nul 2>&1
    )
    timeout /t 2 >nul
    echo ✅ Процеси зупинено
) else (
    echo ✅ Порт 8081 вільний
)

echo 🔨 Компіляція...
javac NalyvatorWebServer.java
if %errorlevel% neq 0 (
    echo ❌ Помилка компіляції!
    pause
    exit /b 1
)
echo ✅ Компіляція успішна!

echo 🚀 Запуск сервера...
echo 📱 Автоматичне відкриття браузера...
echo 🛑 Для зупинки натисніть Ctrl+C
echo.

REM Запускаємо сервер у фоновому режимі
start /B java NalyvatorWebServer

REM Чекаємо 3 секунди для запуску сервера
timeout /t 3 >nul

REM Відкриваємо браузер
start http://localhost:8081

echo ✅ Браузер відкрито!
echo 🔄 Очікування завершення роботи сервера...
echo.

REM Чекаємо завершення роботи сервера
wait 