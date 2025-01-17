// кнопки-крутилки

void encTick() {
  enc.tick();
  if (enc.isTurn()) {
    volumeChanged = true;
    timeoutReset();
    if (enc.isLeft()) {
      thisVolume += 5;
      thisVolume = constrain(thisVolume, 5, 1000);
    }
    if (enc.isRight()) {
      thisVolume -= 5;
      thisVolume = constrain(thisVolume, 5, 1000);
    }

    // Перевірка допустимості значення thisVolume
    if (thisVolume < 5 || thisVolume > 1000) {
      thisVolume = 100;  // Якщо значення поза межами, ставимо значення за замовчуванням
    }

    EEPROM.put(0, thisVolume);  // Записуємо в EEPROM

    dispMode();
  }
}

void btnTick() {
  if (btn.holded()) {
    timeoutReset();
    workMode = !workMode;
    dispMode();
  }
  if (encBtn.holded()) {
    pumpON();
    while (!digitalRead(ENC_SW));
    timeoutReset();
    pumpOFF();
  }  
}
