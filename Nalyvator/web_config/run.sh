#!/bin/bash
echo "🌐 Компіляція Nalyvator Web Server..."
javac NalyvatorWebServer.java
if [ $? -ne 0 ]; then
    echo "❌ Помилка компіляції!"
    exit 1
fi
echo "✅ Компіляція успішна!"
echo "🚀 Запуск сервера..."
java NalyvatorWebServer 