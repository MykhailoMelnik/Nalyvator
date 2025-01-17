void setup() {
  // Ініціалізація серіального монітора для налагодження
#if (DEBUG_UART == 1)
  Serial.begin(9600);
  DEBUG("start");
#endif

  // Перевірка та ініціалізація EEPROM
  if (EEPROM.read(1000) != 10) {
    EEPROM.write(1000, 10);
    EEPROM.put(0, thisVolume);
  }
  EEPROM.get(0, thisVolume);

  // Перевірка допустимості значення thisVolume
  if (thisVolume < 5 || thisVolume > 1000) {
    thisVolume = 100;  // Якщо значення поза межами, ставимо значення за замовчуванням
  }
  EEPROM.put(0, thisVolume);      // Записуємо в EEPROM, якщо значення було змінене
  if (EEPROM.read(1000) != 42) {  // Унікальна мітка для перевірки ініціалізації
    EEPROM.write(1000, 42);       // Записуємо мітку
    for (byte i = 0; i < NUM_SHOTS; i++) {
      EEPROM.write(i, shotPos[i]);  // Записуємо початкові позиції
    }
  }
  // Ініціалізація серво, дисплея та інших компонентів
  loadPositionsFromEEPROM();  // Завантажуємо збережені позиції з EEPROM


  // тыкаем ленту
  strip.setBrightness(130);
  strip.clear();
  strip.show();
  DEBUG("strip init");

  // настройка пинов
  pinMode(PUMP_POWER, 1);
  pinMode(SERVO_POWER, 1);
  for (byte i = 0; i < NUM_SHOTS; i++) {
    if (SWITCH_LEVEL == 0) pinMode(SW_pins[i], INPUT_PULLUP);
  }

  // старт дисплея
  disp.clear();
  disp.brightness(7);
  DEBUG("disp init");

  // настройка серво
  servoON();
  servo.attach(SERVO_PIN, 600 + startServoPoint, 2400);
  if (INVERSE_SERVO) servo.setDirection(REVERSE);

  servo.write(0);
  delay(800);
  servo.setTargetDeg(0);
  servo.setSpeed(60);
  servo.setAccel(0.3);
  servoOFF();

  serviceMode();   // калибровка
  dispMode();      // выводим на дисплей стандартные значения
  timeoutReset();  // сброс таймаута
  TIMEOUTtimer.start();
}
