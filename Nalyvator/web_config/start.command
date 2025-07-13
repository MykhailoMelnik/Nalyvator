#!/bin/bash

# Змінюємо на директорію скрипта
cd "$(dirname "$0")"

echo "🌐 Nalyvator Config - Автозапуск"
echo "================================"

# Перевіряємо чи встановлена Java
if ! command -v java &> /dev/null; then
    echo "❌ Помилка: Java не встановлена!"
    echo "Встановіть Java з https://java.com"
    read -p "Натисніть Enter для виходу..."
    exit 1
fi

echo "✅ Java знайдена: $(java -version 2>&1 | head -n 1)"

# Компілюємо програму
echo "🔨 Компіляція..."
javac NalyvatorWebServer.java
if [ $? -ne 0 ]; then
    echo "❌ Помилка компіляції!"
    read -p "Натисніть Enter для виходу..."
    exit 1
fi

echo "✅ Компіляція успішна!"

# Перевіряємо і зупиняємо процеси на порту 8081
echo "🔍 Перевірка порту 8081..."
PORT_PID=$(lsof -ti:8081 2>/dev/null)
if [ ! -z "$PORT_PID" ]; then
    echo "🛑 Зупиняємо процес $PORT_PID на порту 8081..."
    kill $PORT_PID 2>/dev/null
    sleep 1
    echo "✅ Процес зупинено"
else
    echo "✅ Порт 8081 вільний"
fi

# Запускаємо сервер у фоновому режимі
echo "🚀 Запуск сервера..."
java NalyvatorWebServer &
SERVER_PID=$!

# Чекаємо 2 секунди
sleep 2

# Відкриваємо браузер
echo "🌐 Відкриття браузера..."
open http://localhost:8081

echo "✅ Сервер запущено на http://localhost:8081"
echo "🛑 Для зупинки сервера закрийте це вікно або натисніть Ctrl+C"
echo ""

# Чекаємо поки користувач не закриє
wait $SERVER_PID 