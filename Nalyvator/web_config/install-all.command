#!/bin/bash

echo "🔧 Автоматичне встановлення Java та Arduino CLI"
echo "==============================================="

# Перевірка Java
echo "🔍 Перевірка Java..."
if command -v java &> /dev/null; then
    echo "✅ Java вже встановлена"
else
    echo "❌ Java не встановлена"
    echo "📥 Встановлення Java через Homebrew..."
    
    # Перевірка Homebrew
    if ! command -v brew &> /dev/null; then
        echo "📦 Встановлення Homebrew..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
    
    echo "🍺 Встановлення OpenJDK..."
    brew install openjdk@17
    
    # Додавання Java до PATH
    echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
    echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
    source ~/.zshrc
    
    echo "✅ Java встановлена!"
fi

# Перевірка Arduino CLI
echo ""
echo "🔍 Перевірка Arduino CLI..."
if command -v arduino-cli &> /dev/null; then
    echo "✅ Arduino CLI вже встановлений"
else
    echo "❌ Arduino CLI не встановлений"
    echo "📥 Встановлення Arduino CLI..."
    
    # Завантаження та встановлення Arduino CLI
    curl -fsSL https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh | sh
    
    # Додавання до PATH
    echo 'export PATH="$HOME/bin:$PATH"' >> ~/.zshrc
    source ~/.zshrc
    
    echo "✅ Arduino CLI встановлений!"
fi

# Встановлення бібліотек
echo ""
echo "📚 Встановлення Arduino бібліотек..."
echo ""

echo "🔧 Встановлення Servo..."
arduino-cli lib install Servo
if [ $? -eq 0 ]; then
    echo "✅ Servo встановлено"
else
    echo "❌ Помилка встановлення Servo"
fi

echo "🔧 Встановлення GyverTM1637..."
arduino-cli lib install GyverTM1637
if [ $? -eq 0 ]; then
    echo "✅ GyverTM1637 встановлено"
else
    echo "❌ Помилка встановлення GyverTM1637"
fi

echo "🔧 Встановлення ServoSmooth..."
arduino-cli lib install ServoSmooth
if [ $? -eq 0 ]; then
    echo "✅ ServoSmooth встановлено"
else
    echo "❌ Помилка встановлення ServoSmooth"
fi

echo ""
echo "✅ Всі компоненти встановлено!"
echo "🚀 Тепер можна запускати start.command"
echo ""
read -p "Натисніть Enter для завершення..." 