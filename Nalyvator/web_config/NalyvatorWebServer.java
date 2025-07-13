import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;
import java.util.*;

public class NalyvatorWebServer {
    private static final int PORT = 8081;
    private static final String INO_FILE = "../Nalyvator.ino";
    
    public static void main(String[] args) {
        System.out.println("🌐 Запуск Nalyvator Web Server...");
        
        // Автоматично зупиняємо процеси на порту 8081
        try {
            System.out.println("🔍 Перевірка порту " + PORT + "...");
            ProcessBuilder killPb = new ProcessBuilder("lsof", "-ti:" + PORT);
            Process killProcess = killPb.start();
            
            BufferedReader killReader = new BufferedReader(new InputStreamReader(killProcess.getInputStream()));
            String pid = killReader.readLine();
            
            if (pid != null && !pid.trim().isEmpty()) {
                System.out.println("🛑 Зупиняємо процес " + pid + " на порту " + PORT);
                ProcessBuilder stopPb = new ProcessBuilder("kill", pid.trim());
                stopPb.start().waitFor();
                
                // Даємо час процесу завершитися
                Thread.sleep(1000);
                System.out.println("✅ Процес зупинено");
            } else {
                System.out.println("✅ Порт " + PORT + " вільний");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Помилка при перевірці порту: " + e.getMessage());
        }
        
        System.out.println("📱 Відкрийте браузер і перейдіть на: http://localhost:" + PORT);
        System.out.println("🛑 Для зупинки сервера натисніть Ctrl+C");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleRequest(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Помилка сервера: " + e.getMessage());
        }
    }
    
    private static void handleRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            String requestLine = in.readLine();
            if (requestLine == null) return;
            
            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path = parts[1];
            
            if (method.equals("GET") && path.equals("/")) {
                sendHtmlResponse(out);
            } else if (method.equals("GET") && path.equals("/api/angles")) {
                sendAnglesResponse(out);
            } else if (method.equals("POST") && path.equals("/api/angles")) {
                handleUpdateAngles(in, out);
            } else if (method.equals("GET") && path.equals("/api/ports")) {
                sendPortsResponse(out);
            } else if (method.equals("POST") && path.equals("/api/upload")) {
                handleUploadToArduino(in, out);
            } else {
                send404Response(out);
            }
            
        } catch (IOException e) {
            System.err.println("Помилка обробки запиту: " + e.getMessage());
        }
    }
    
    private static void sendHtmlResponse(PrintWriter out) {
        String html = """
            <!DOCTYPE html>
            <html lang="uk">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Nalyvator Config</title>
                <style>
                            body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 10px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            overflow: hidden;
        }
        .container {
            background: white;
            padding: 20px;
            border-radius: 20px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
            max-width: 1200px;
            margin: 0 auto;
            backdrop-filter: blur(10px);
        }
        .container.flex {
            display: flex;
            gap: 40px;
        }
                    .left-panel {
                        flex: 1;
                    }
                    .right-panel {
                        width: 300px;
                    }
                    h1 {
                        color: #333;
                        text-align: center;
                        margin-bottom: 10px;
                        font-size: 24px;
                    }
                    .section {
                        margin: 3px 0;
                        padding: 15px;
                        background: linear-gradient(145deg, #f8f9fa, #e9ecef);
                        border-radius: 15px;
                        border: 2px solid #6f42c1;
                        box-shadow: 0 8px 25px rgba(0,0,0,0.08);
                    }
                    .section h3 {
                        margin: 0 0 15px 0;
                        color: #2c3e50;
                        font-size: 18px;
                        font-weight: 600;
                        display: flex;
                        align-items: center;
                        gap: 10px;
                    }
                    .shot-row {
                        display: flex;
                        align-items: center;
                        margin: 8px 0;
                        padding: 10px;
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.05);
                        border: 1px solid #f1f3f4;
                    }
                    .shot-label {
                        width: 120px;
                        font-weight: bold;
                        color: #555;
                    }
                    .angle-controls {
                        display: flex;
                        align-items: center;
                        gap: 5px;
                    }
                    .angle-btn {
                        background: #e9ecef;
                        color: #495057;
                        border: 1px solid #ced4da;
                        border-radius: 4px;
                        padding: 6px 10px;
                        font-size: 12px;
                        cursor: pointer;
                        font-weight: 500;
                    }
                    .angle-btn:hover {
                        background: #dee2e6;
                        border-color: #adb5bd;
                    }
                    input[type="number"] {
                        width: 90px;
                        padding: 10px 12px;
                        border: 2px solid #e9ecef;
                        border-radius: 8px;
                        font-size: 16px;
                        text-align: center;
                        font-weight: 500;
                        transition: all 0.2s ease;
                        background: #fafbfc;
                    }
                    input[type="number"]:focus {
                        outline: none;
                        border-color: #6f42c1;
                        background: white;
                        box-shadow: 0 0 0 3px rgba(111, 66, 193, 0.1);
                    }
                    .degrees {
                        margin-left: 10px;
                        color: #666;
                    }
                    .save-btn {
                        background: #4CAF50;
                        color: white;
                        padding: 12px 30px;
                        border: none;
                        border-radius: 5px;
                        font-size: 16px;
                        cursor: pointer;
                        margin-top: 20px;
                        width: 100%;
                        position: sticky;
                        top: 20px;
                        z-index: 100;
                    }
                    .save-btn:hover {
                        background: #45a049;
                    }
                    .status {
                        margin-top: 20px;
                        padding: 10px;
                        border-radius: 5px;
                        text-align: center;
                        display: none;
                    }
                    .success {
                        background: #d4edda;
                        color: #155724;
                        border: 1px solid #c3e6cb;
                    }
                    .error {
                        background: #f8d7da;
                        color: #721c24;
                        border: 1px solid #f5c6cb;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1 style="margin-bottom: 0px;">Налаштування Nalyvator</h1>
                    
                    <div class="container flex">
                        <div class="left-panel">
                            <!-- Секція кутів рюмок -->
                            <div class="section">
                                <h3>Кути рюмок</h3>
                                <div id="shots-container"></div>
                            </div>
                            
                            <div id="status" class="status"></div>
                        </div>
                        
                                                <div class="right-panel">
                            <!-- Секція додаткових параметрів -->
                            <div class="section">
                                <h3>Додаткові параметри</h3>
                                <div id="params-container"></div>
                            </div>
                            
                            <!-- Секція вибору порту -->
                            <div class="section">
                                <h3>Порт Arduino</h3>
                                <div class="shot-row">
                                    <div class="shot-label">Порт:</div>
                                    <select id="arduino-port" style="width: 200px; padding: 8px; border: 2px solid #e9ecef; border-radius: 8px;">
                                        <option value="">Завантаження портів...</option>
                                    </select>
                                </div>
                                <div style="text-align: center; margin-top: 10px;">
                                    <button class="angle-btn" onclick="refreshPorts()" style="width: 100%;">🔄 Оновити список портів</button>
                                </div>
                            </div>
                            
                            <button class="save-btn" onclick="saveAndUpload()">💾 Зберегти та прошити (кн. Enter)</button>
                        </div>
                </div>

                <script>
                    let currentAngles = [];
                    let currentTime50ml = 4700;
                    let currentStartServoPoint = 0;
                    
                    async function loadAngles() {
                        try {
                            const response = await fetch('/api/angles');
                            const data = await response.json();
                            if (data.angles) {
                                currentAngles = data.angles;
                                currentTime50ml = data.time50ml || 4700;
                                currentStartServoPoint = data.startServoPoint || 0;
                                createShotInputs();
                            } else {
                                showStatus('❌ Помилка: ' + data.error, false);
                            }
                        } catch (error) {
                            showStatus('❌ Помилка завантаження: ' + error.message, false);
                        }
                    }
                    
                    function createShotInputs() {
                        const shotsContainer = document.getElementById('shots-container');
                        const paramsContainer = document.getElementById('params-container');
                        
                        shotsContainer.innerHTML = '';
                        paramsContainer.innerHTML = '';
                        
                        // Додаємо кути рюмок з кнопками
                        currentAngles.forEach((angle, index) => {
                            const row = document.createElement('div');
                            row.className = 'shot-row';
                            row.innerHTML = `
                                <div class="shot-label">Рюмка ${index + 1}:</div>
                                <div class="angle-controls">
                                    <button class="angle-btn" onclick="changeAngle(${index}, -3)">-3</button>
                                    <button class="angle-btn" onclick="changeAngle(${index}, -2)">-2</button>
                                    <button class="angle-btn" onclick="changeAngle(${index}, -1)">-1</button>
                                    <input type="number" id="angle${index}" value="${angle}" min="0" max="180">
                                    <button class="angle-btn" onclick="changeAngle(${index}, 1)">+1</button>
                                    <button class="angle-btn" onclick="changeAngle(${index}, 2)">+2</button>
                                    <button class="angle-btn" onclick="changeAngle(${index}, 3)">+3</button>
                                </div>
                                <div class="degrees">градусів</div>
                            `;
                            shotsContainer.appendChild(row);
                        });
                        
                        // Додаємо time50ml
                        const timeRow = document.createElement('div');
                        timeRow.className = 'shot-row';
                        timeRow.innerHTML = `
                            <div class="shot-label">Час 50мл:</div>
                            <input type="number" id="time50ml" value="${currentTime50ml}" min="1000" max="10000" step="100">
                            <div class="degrees">мс</div>
                        `;
                        paramsContainer.appendChild(timeRow);
                        
                        // Додаємо startServoPoint
                        const servoRow = document.createElement('div');
                        servoRow.className = 'shot-row';
                        servoRow.innerHTML = `
                            <div class="shot-label">Старт серво:</div>
                            <input type="number" id="startServoPoint" value="${currentStartServoPoint}" min="-100" max="100">
                            <div class="degrees">градусів</div>
                        `;
                        paramsContainer.appendChild(servoRow);
                    }
                    
                    function changeAngle(index, delta) {
                        const input = document.getElementById(`angle${index}`);
                        let newValue = parseInt(input.value) + delta;
                        
                        // Обмежуємо значення
                        if (newValue < 0) newValue = 0;
                        if (newValue > 180) newValue = 180;
                        
                        input.value = newValue;
                    }
                    

                    
                                        async function saveChanges() {
                        const newAngles = [];
                        let hasError = false;
                        
                        // Збираємо кути рюмок
                        for (let i = 0; i < currentAngles.length; i++) {
                            const input = document.getElementById(`angle${i}`);
                            const value = parseInt(input.value);
                            
                            if (isNaN(value) || value < 0 || value > 180) {
                                showStatus(`Помилка: Рюмка ${i + 1} має некоректне значення (0-180)`, false);
                                hasError = true;
                                break;
                            }
                            newAngles.push(value);
                        }
                        
                        if (hasError) return;
                        
                        // Збираємо time50ml
                        const time50mlInput = document.getElementById('time50ml');
                        const time50ml = parseInt(time50mlInput.value);
                        if (isNaN(time50ml) || time50ml < 1000 || time50ml > 10000) {
                            showStatus('Помилка: Час 50мл має бути від 1000 до 10000 мс', false);
                            return;
                        }
                        
                        // Збираємо startServoPoint
                        const startServoInput = document.getElementById('startServoPoint');
                        const startServoPoint = parseInt(startServoInput.value);
                        if (isNaN(startServoPoint) || startServoPoint < -100 || startServoPoint > 100) {
                            showStatus('Помилка: Старт серво має бути від -100 до 100 градусів', false);
                            return;
                        }
                        
                        // Зберігаємо зміни відразу без підтвердження
                        
                        try {
                            const response = await fetch('/api/angles', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify({ 
                                    angles: newAngles,
                                    time50ml: time50ml,
                                    startServoPoint: startServoPoint
                                })
                            });
                            
                            const result = await response.json();
                            
                            if (result.success) {
                                showStatus('✅ ' + result.message, true);
                                currentAngles = newAngles;
                                currentTime50ml = time50ml;
                                currentStartServoPoint = startServoPoint;
                            } else {
                                showStatus('❌ Помилка: ' + result.error, false);
                            }
                        } catch (error) {
                            showStatus("❌ Помилка з'єднання: " + error.message, false);
                        }
                    }
                    
                    function showStatus(message, isSuccess) {
                        const status = document.getElementById('status');
                        status.textContent = message;
                        status.className = 'status ' + (isSuccess ? 'success' : 'error');
                        status.style.display = 'block';
                        
                        if (isSuccess) {
                            setTimeout(() => {
                                status.style.display = 'none';
                            }, 5000);
                        }
                    }
                    
                    window.onload = function() {
                        loadAngles();
                        setTimeout(loadPorts, 100);
                    };
                    
                                        async function loadPorts() {
                        try {
                            console.log('Завантаження портів...');
                            const response = await fetch('/api/ports');
                            const data = await response.json();
                            console.log('Отримані дані:', data);
                            
                            const portSelect = document.getElementById('arduino-port');
                            if (!portSelect) {
                                console.error('Елемент arduino-port не знайдено');
                                return;
                            }
                            portSelect.innerHTML = '';
                            
                            if (data.detected_ports && data.detected_ports.length > 0) {
                                console.log('Знайдено портів:', data.detected_ports.length);
                                data.detected_ports.forEach(item => {
                                    const port = item.port;
                                    console.log('Перевіряємо порт:', port.address, port.protocol_label);
                                    // Показуємо тільки USB порти або порти з usbserial
                                    if (port.protocol_label.includes('USB') || port.address.includes('usbserial')) {
                                        const option = document.createElement('option');
                                        // Конвертуємо cu в tty для arduino-cli
                                        const ttyPort = port.address.replace('/dev/cu.', '/dev/tty.');
                                        option.value = ttyPort;
                                        option.textContent = `${ttyPort} (${port.protocol_label})`;
                                        portSelect.appendChild(option);
                                        console.log('Додано порт:', ttyPort);
                                    }
                                });
                                
                                // Якщо не знайдено USB портів, показуємо всі
                                if (portSelect.options.length === 0) {
                                    data.detected_ports.forEach(item => {
                                        const port = item.port;
                                        const option = document.createElement('option');
                                        // Конвертуємо cu в tty для arduino-cli
                                        const ttyPort = port.address.replace('/dev/cu.', '/dev/tty.');
                                        option.value = ttyPort;
                                        option.textContent = `${ttyPort} (${port.protocol_label})`;
                                        portSelect.appendChild(option);
                                    });
                                }
                            } else {
                                const option = document.createElement('option');
                                option.value = '';
                                option.textContent = 'Порти не знайдено';
                                portSelect.appendChild(option);
                            }
                        } catch (error) {
                            console.error('Помилка завантаження портів:', error);
                        }
                    }
                    
                    function refreshPorts() {
                        loadPorts();
                    }
                    
                    // Додаємо обробник натискання Enter
                    document.addEventListener('keydown', function(event) {
                        if (event.key === 'Enter') {
                            event.preventDefault();
                            saveAndUpload();
                        }
                    });
                    
                    async function saveAndUpload() {
                        // Спочатку зберігаємо зміни
                        await saveChanges();
                        
                        // Отримуємо вибраний порт
                        const portSelect = document.getElementById('arduino-port');
                        const selectedPort = portSelect.value;
                        
                        if (!selectedPort) {
                            showStatus('❌ Виберіть порт Arduino', false);
                            return;
                        }
                        
                        // Потім прошиваємо Arduino
                        try {
                            showStatus('🔄 Компіляція та прошивка Arduino...', true);
                            const response = await fetch('/api/upload', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify({ port: selectedPort })
                            });
                            
                            const result = await response.json();
                            
                            if (result.success) {
                                showStatus('✅ ' + result.message, true);
                            } else {
                                showStatus('❌ Помилка прошивки: ' + result.error, false);
                            }
                        } catch (error) {
                            showStatus('❌ Помилка прошивки: ' + error.message, false);
                        }
                    }
                </script>
            </body>
            </html>
            """;
        
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html; charset=UTF-8");
        out.println("Content-Length: " + html.getBytes(StandardCharsets.UTF_8).length);
        out.println();
        out.println(html);
    }
    
    private static void sendAnglesResponse(PrintWriter out) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(INO_FILE)));
            
            // Знаходимо кути
            Pattern anglePattern = Pattern.compile("const byte shotPos\\[\\] = \\{([^}]+)\\}");
            Matcher angleMatcher = anglePattern.matcher(content);
            
            // Знаходимо time50ml
            Pattern timePattern = Pattern.compile("const long time50ml = (\\d+);");
            Matcher timeMatcher = timePattern.matcher(content);
            
            // Знаходимо startServoPoint
            Pattern servoPattern = Pattern.compile("const long startServoPoint = (-?\\d+);");
            Matcher servoMatcher = servoPattern.matcher(content);
            
            if (angleMatcher.find()) {
                String[] parts = angleMatcher.group(1).split(",");
                List<Integer> angles = new ArrayList<>();
                for (String part : parts) {
                    angles.add(Integer.parseInt(part.trim()));
                }
                
                // Отримуємо значення time50ml
                long time50ml = 4700; // значення за замовчуванням
                if (timeMatcher.find()) {
                    time50ml = Long.parseLong(timeMatcher.group(1));
                }
                
                // Отримуємо значення startServoPoint
                long startServoPoint = 0; // значення за замовчуванням
                if (servoMatcher.find()) {
                    startServoPoint = Long.parseLong(servoMatcher.group(1));
                }
                
                String json = "{\"angles\": " + angles.toString() + 
                             ", \"time50ml\": " + time50ml + 
                             ", \"startServoPoint\": " + startServoPoint + "}";
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Content-Length: " + json.getBytes(StandardCharsets.UTF_8).length);
                out.println();
                out.println(json);
            } else {
                sendErrorResponse(out, "Не знайдено масив shotPos");
            }
        } catch (Exception e) {
            sendErrorResponse(out, e.getMessage());
        }
    }
    
    private static void handleUpdateAngles(BufferedReader in, PrintWriter out) {
        try {
            // Читаємо заголовки
            String line;
            int contentLength = 0;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring(16).trim());
                }
            }
            
            // Читаємо тіло запиту
            char[] body = new char[contentLength];
            in.read(body, 0, contentLength);
            String jsonBody = new String(body);
            
            // Парсимо JSON (спрощено)
            List<Integer> angles = new ArrayList<>();
            long time50ml = 4700;
            long startServoPoint = 0;
            
            // Парсимо кути
            Pattern anglePattern = Pattern.compile("\"angles\":\\s*\\[(.*?)\\]");
            Matcher angleMatcher = anglePattern.matcher(jsonBody);
            if (angleMatcher.find()) {
                String[] parts = angleMatcher.group(1).split(",");
                for (String part : parts) {
                    angles.add(Integer.parseInt(part.trim()));
                }
            }
            
            // Парсимо time50ml
            Pattern timePattern = Pattern.compile("\"time50ml\":\\s*(\\d+)");
            Matcher timeMatcher = timePattern.matcher(jsonBody);
            if (timeMatcher.find()) {
                time50ml = Long.parseLong(timeMatcher.group(1));
            }
            
            // Парсимо startServoPoint
            Pattern servoPattern = Pattern.compile("\"startServoPoint\":\\s*(-?\\d+)");
            Matcher servoMatcher = servoPattern.matcher(jsonBody);
            if (servoMatcher.find()) {
                startServoPoint = Long.parseLong(servoMatcher.group(1));
            }
            
            // Оновлюємо файл
            String content = new String(Files.readAllBytes(Paths.get(INO_FILE)));
            
            // Оновлюємо кути
            String newAnglesStr = angles.toString().replaceAll("[\\[\\]]", "");
            content = content.replaceAll(
                "const byte shotPos\\[\\] = \\{([^}]+)\\}",
                "const byte shotPos[] = {" + newAnglesStr + "}"
            );
            
            // Оновлюємо time50ml
            content = content.replaceAll(
                "const long time50ml = \\d+;",
                "const long time50ml = " + time50ml + ";"
            );
            
            // Оновлюємо startServoPoint
            content = content.replaceAll(
                "const long startServoPoint = -?\\d+;",
                "const long startServoPoint = " + startServoPoint + ";"
            );
            
            Files.write(Paths.get(INO_FILE), content.getBytes());
            
            String response = "{\"success\": true, \"message\": \"Параметри успішно оновлено!\"}";
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: application/json");
            out.println("Content-Length: " + response.getBytes(StandardCharsets.UTF_8).length);
            out.println();
            out.println(response);
            
        } catch (Exception e) {
            sendErrorResponse(out, e.getMessage());
        }
    }
    
    private static void sendErrorResponse(PrintWriter out, String error) {
        String response = "{\"error\": \"" + error + "\"}";
        out.println("HTTP/1.1 500 Internal Server Error");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + response.getBytes(StandardCharsets.UTF_8).length);
        out.println();
        out.println(response);
    }
    
    private static void sendPortsResponse(PrintWriter out) {
        try {
            ProcessBuilder pb = new ProcessBuilder("arduino-cli", "board", "list", "--format", "json");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Content-Length: " + output.toString().getBytes(StandardCharsets.UTF_8).length);
                out.println();
                out.println(output.toString());
            } else {
                sendErrorResponse(out, "Помилка отримання списку портів");
            }
        } catch (Exception e) {
            sendErrorResponse(out, "Помилка: " + e.getMessage());
        }
    }
    
    private static void handleUploadToArduino(BufferedReader in, PrintWriter out) {
        try {
            System.out.println("🔄 Початок обробки запиту прошивки...");
            
            // Читаємо заголовки
            String line;
            int contentLength = 0;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring(16).trim());
                    System.out.println("📏 Content-Length: " + contentLength);
                }
            }
            
            // Читаємо тіло запиту
            char[] body = new char[contentLength];
            in.read(body, 0, contentLength);
            String jsonBody = new String(body);
            System.out.println("📄 JSON body: " + jsonBody);
            
            // Парсимо порт
            Pattern portPattern = Pattern.compile("\"port\":\\s*\"([^\"]+)\"");
            Matcher portMatcher = portPattern.matcher(jsonBody);
            String port = "/dev/tty.usbserial-1320"; // значення за замовчуванням
            
            if (portMatcher.find()) {
                port = portMatcher.group(1);
                System.out.println("🔌 Вибраний порт: " + port);
            } else {
                System.out.println("⚠️ Порт не знайдено в JSON, використовуємо за замовчуванням: " + port);
            }
            
            // Компілюємо та прошиваємо Arduino
            System.out.println("🔨 Початок компіляції...");
            ProcessBuilder pb = new ProcessBuilder(
                "arduino-cli", "compile", "--fqbn", "arduino:avr:nano", "Nalyvator.ino"
            );
            pb.directory(new File("../"));
            System.out.println("📁 Робоча директорія: " + pb.directory().getAbsolutePath());
            Process process = pb.start();
            
            // Читаємо вивід процесу
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            String outputLine;
            while ((outputLine = reader.readLine()) != null) {
                output.append(outputLine).append("\n");
            }
            
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Компіляція успішна, тепер прошиваємо
                ProcessBuilder uploadPb = new ProcessBuilder(
                    "arduino-cli", "upload", "--fqbn", "arduino:avr:nano", "--port", port, "Nalyvator.ino"
                );
                uploadPb.directory(new File("../"));
                Process uploadProcess = uploadPb.start();
                
                BufferedReader uploadReader = new BufferedReader(new InputStreamReader(uploadProcess.getInputStream()));
                BufferedReader uploadErrorReader = new BufferedReader(new InputStreamReader(uploadProcess.getErrorStream()));
                
                StringBuilder uploadOutput = new StringBuilder();
                StringBuilder uploadErrorOutput = new StringBuilder();
                
                while ((outputLine = uploadReader.readLine()) != null) {
                    uploadOutput.append(outputLine).append("\n");
                }
                
                while ((errorLine = uploadErrorReader.readLine()) != null) {
                    uploadErrorOutput.append(errorLine).append("\n");
                }
                
                int uploadExitCode = uploadProcess.waitFor();
                
                if (uploadExitCode == 0) {
                    String response = "{\"success\": true, \"message\": \"Arduino успішно прошито!\"}";
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: application/json");
                    out.println("Content-Length: " + response.getBytes(StandardCharsets.UTF_8).length);
                    out.println();
                    out.println(response);
                } else {
                    String response = "{\"success\": false, \"error\": \"Помилка прошивки: " + uploadErrorOutput.toString() + "\"}";
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: application/json");
                    out.println("Content-Length: " + response.getBytes(StandardCharsets.UTF_8).length);
                    out.println();
                    out.println(response);
                }
            } else {
                String response = "{\"success\": false, \"error\": \"Помилка компіляції: " + errorOutput.toString() + "\"}";
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Content-Length: " + response.getBytes(StandardCharsets.UTF_8).length);
                out.println();
                out.println(response);
            }
            
        } catch (Exception e) {
            sendErrorResponse(out, "Помилка прошивки: " + e.getMessage());
        }
    }
    
    private static void send404Response(PrintWriter out) {
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/plain");
        out.println();
        out.println("404 Not Found");
    }
} 