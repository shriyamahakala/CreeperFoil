#include <SoftwareSerial.h>

#define LED 10
#define BT_TX_PIN 11
#define BT_RX_PIN 10
#define anPin 0

// Bluetooth
byte input = 0; // Value passed in by the bluetooth
float output = 0;
SoftwareSerial bluetoothModule =  SoftwareSerial(BT_RX_PIN, BT_TX_PIN);



void setup() {
  // put your setup code here, to run once:
  -

  bluetoothModule.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  while (bluetoothModule.available() > 0) {
    input = bluetoothModule.read();
  }

  if(frames%100000 == 0) { // Every 100000 frames send the average of the last 100 values (then reset and restart)
    output = //smth motion sensor;
    #sendData(output);
    bluetoothModule.print(' ' + output + '#'); //+ time??
    frames=0;
  }
  
  frames++;

  
}

void sendData(float value) {
  String string = String(value); // Convert to string and send
  bluetoothModule.print(' ' + string + '#'); // With the character "#" the app understands the sending is finished
}
