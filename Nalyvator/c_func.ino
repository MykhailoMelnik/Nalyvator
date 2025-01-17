// различные функции
#include <EEPROM.h>  // Підключаємо бібліотеку для роботи з EEPROM
byte shotPosSerw[NUM_SHOTS];

void savePositionsToEEPROM() {
  for (byte i = 0; i < NUM_SHOTS; i++) {
    EEPROM.write(i, shotPosSerw[i]);  // Записуємо нові позиції
  }
}

void loadPositionsFromEEPROM() {
  for (byte i = 0; i < NUM_SHOTS; i++) {
    byte pos = EEPROM.read(i);      // Зчитуємо значення
    if (pos < 0 || pos > 180) {     // Якщо значення некоректне
      shotPosSerw[i] = shotPos[i];  // Використовуємо значення за замовчуванням
    } else {
      shotPosSerw[i] = pos;  // Використовуємо значення з EEPROM
    }
  }
}


void serviceMode() {
  if (!digitalRead(BTN_PIN)) {
    byte serviceText[] = { _S, _E, _r, _U, _i, _C, _E };
    disp.runningString(serviceText, sizeof(serviceText), 150);
    while (!digitalRead(BTN_PIN))
      ;  // ждём отпускания
    delay(200);
    servoON();
    int servoPos = 0;
    long pumpTime = 0;
    timerMinim timer100(100);
    disp.displayInt(0);
    bool flag;
    for (;;) {
      servo.tick();
      enc.tick();

      if (timer100.isReady()) {  // период 100 мс
        // работа помпы со счётчиком
        if (!digitalRead(ENC_SW)) {
          if (flag) pumpTime += 100;
          else pumpTime = 0;
          disp.displayInt(pumpTime);
          pumpON();
          flag = true;
        } else {
          pumpOFF();
          flag = false;
        }

        // зажигаем светодиоды от кнопок
        for (byte i = 0; i < NUM_SHOTS; i++) {
          if (!digitalRead(SW_pins[i])) {
            strip.setLED(i, mCOLOR(GREEN));
          } else {
            strip.setLED(i, mCOLOR(BLACK));
          }
          strip.show();
        }
      }

      if (enc.isTurn()) {
        // крутим серво от энкодера
        pumpTime = 0;
        if (enc.isLeft()) {
          servoPos += 2;
        }
        if (enc.isRight()) {
          servoPos -= 2;
        }
        servoPos = constrain(servoPos, 0, 180);
        disp.displayInt(servoPos);
        servo.setTargetDeg(servoPos);
      }

      if (btn.holded()) {
        servo.setTargetDeg(0);
        break;
      }
    }
  }
  disp.clear();
  while (!servo.tick())
    ;
  servoOFF();
}

// выводим объём и режим
void dispMode() {
  disp.displayInt(thisVolume);
  if (workMode) disp.displayByte(0, _A);
  else {
    disp.displayByte(0, _P);
    pumpOFF();
  }
}

// наливайка, опрос кнопок
void flowTick() {
  if (FLOWdebounce.isReady()) {
    for (byte i = 0; i < NUM_SHOTS; i++) {
      bool swState = !digitalRead(SW_pins[i]) ^ SWITCH_LEVEL;
      if (swState && shotStates[i] == NO_GLASS) {  // поставили пустую рюмку
        timeoutReset();                            // сброс таймаута
        shotStates[i] = EMPTY;                     // флаг на заправку
        strip.setLED(i, mCOLOR(RED));              // подсветили
        LEDchanged = true;
        DEBUG("set glass");
        DEBUG(i);
      }
      if (!swState && shotStates[i] != NO_GLASS) {  // убрали пустую/полную рюмку
        shotStates[i] = NO_GLASS;                   // статус - нет рюмки
        strip.setLED(i, mCOLOR(BLACK));             // нигра
        LEDchanged = true;
        timeoutReset();  // сброс таймаута
        if (i == curPumping) {
          curPumping = -1;     // снимаем выбор рюмки
          systemState = WAIT;  // режим работы - ждать
          WAITtimer.reset();
          pumpOFF();  // помпу выкл
        }
        DEBUG("take glass");
        DEBUG(i);
      }
    }

    if (workMode) {         // авто
      flowRoutnie();        // крутим отработку кнопок и поиск рюмок
    } else {                // ручной
      if (btn.clicked()) {  // клик!
        systemON = true;    // система активирована
        timeoutReset();     // таймаут сброшен
      }
      if (systemON) flowRoutnie();  // если активны - ищем рюмки и всё такое
    }
  }
}

// поиск и заливка
void flowRoutnie() {
  if (systemState == SEARCH) {  // если поиск рюмки
    bool noGlass = true;
    if (sizeof(shotPosSerw) / sizeof(shotPosSerw[0]) > 0) {
      for (byte i = 0; i < NUM_SHOTS; i++) {
        if (shotStates[i] == EMPTY && i != curPumping) {  // шукаємо рюмку
          noGlass = false;                                // рюмка знайдена
          parking = false;
          curPumping = i;                       // запам'ятовуємо рюмку
          systemState = MOVING;                 // режим - рух до рюмки
          shotStates[curPumping] = IN_PROCESS;  // статус - у процесі
          servoON();                            // включаємо серво
          servo.attach();

          int correction = previousShot;  // Значення корекції для повернення назад
          int targetPos = shotPos[curPumping];

          // Додаємо корекцію тільки при русі назад (менший номер рюмки)
          if (shotPos[curPumping] < servo.getCurrentDeg()) {
            targetPos -= correction;  // Зменшуємо цільову позицію
          }

          servo.setTargetDeg(constrain(targetPos, 0, 180));  // Обмежуємо позицію в межах 0-180         DEBUG("find glass");
          DEBUG(curPumping);
          break;
        }
      }
    } else {
      for (byte i = 0; i < NUM_SHOTS; i++) {
        if (shotStates[i] == EMPTY && i != curPumping) {  // поиск
          noGlass = false;                                // флаг что нашли хоть одну рюмку
          parking = false;
          curPumping = i;                       // запоминаем выбор
          systemState = MOVING;                 // режим - движение
          shotStates[curPumping] = IN_PROCESS;  // стакан в режиме заполнения
          servoON();                            // вкл питание серво
          servo.attach();
          servo.setTargetDeg(shotPos[curPumping]);  // задаём цель
          DEBUG("find glass");
          DEBUG(curPumping);
          break;
        }
      }
    }
    if (noGlass && !parking) {  // если не нашли ни одной рюмки
      servoON();
      servo.setTargetDeg(0);  // цель серво - 0
      if (servo.tick()) {     // едем до упора
        servoOFF();           // выключили серво
        systemON = false;     // выключили систему
        parking = true;
        DEBUG("no glass");
      }
    }
  } else if (systemState == MOVING) {                           // движение к рюмке
    if (servo.tick()) {                                         // если приехали
      systemState = PUMPING;                                    // режим - наливание
      FLOWtimer.setInterval((long)thisVolume * time50ml / 50);  // перенастроили таймер
      FLOWtimer.reset();                                        // сброс таймера
      pumpON();                                                 // НАЛИВАЙ!
      strip.setLED(curPumping, mCOLOR(YELLOW));                 // зажгли цвет
      strip.show();
      DEBUG("fill glass");
      DEBUG(curPumping);
    }

  } else if (systemState == PUMPING) {         // если качаем
    if (FLOWtimer.isReady()) {                 // если налили (таймер)
      pumpOFF();                               // помпа выкл
      shotStates[curPumping] = READY;          // налитая рюмка, статус: готов
      strip.setLED(curPumping, mCOLOR(LIME));  // подсветили
      strip.show();
      curPumping = -1;     // снимаем выбор рюмки
      systemState = WAIT;  // режим работы - ждать
      WAITtimer.reset();
      DEBUG("wait");
    }
  } else if (systemState == WAIT) {
    if (WAITtimer.isReady()) {  // подождали после наливания
      systemState = SEARCH;
      timeoutReset();
      DEBUG("search");
    }
  }
}

// отрисовка светодиодов по флагу (100мс)
void LEDtick() {
  if (LEDchanged && LEDtimer.isReady()) {
    LEDchanged = false;
    strip.show();
  }
}

// сброс таймаута
void timeoutReset() {
  if (!timeoutState) disp.brightness(7);
  timeoutState = true;
  TIMEOUTtimer.reset();
  TIMEOUTtimer.start();
  DEBUG("timeout reset");
}

// сам таймаут
void timeoutTick() {
  if (systemState == SEARCH && timeoutState && TIMEOUTtimer.isReady()) {
    DEBUG("timeout");
    timeoutState = false;
    disp.brightness(1);
    POWEROFFtimer.reset();
    jerkServo();
    if (volumeChanged) {
      volumeChanged = false;
      EEPROM.put(0, thisVolume);
    }
  }

  // дёргаем питание серво, это приводит к скачку тока и powerbank не отключает систему
  if (!timeoutState && TIMEOUTtimer.isReady()) {
    if (!POWEROFFtimer.isReady()) {  // пока не сработал таймер полного отключения
      jerkServo();
    } else {
      disp.clear();
    }
  }
}

void jerkServo() {
  if (KEEP_POWER) {
    disp.brightness(7);
    servoON();
    servo.attach();
    servo.write(random(0, 4));
    delay(200);
    servo.detach();
    servoOFF();
    disp.brightness(1);
  }
}
