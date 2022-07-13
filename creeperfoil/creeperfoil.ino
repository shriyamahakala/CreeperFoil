#include <Wire.h>
#include <Servo.h>
#include <SPI.h>
#include <MFRC522.h>
 
#define SS_PIN 10
#define RST_PIN 9

#define BT_TX_PIN 11
#define BT_RX_PIN 10


const int MPU_ADDR = 0x68; 
const int buttonPin = 4;

int red_light_pin = 5;
int green_light_pin = 6;
int blue_light_pin = 7;
int16_t gyro_x, gyro_y, gyro_z, gyro_x1, gyro_y1, gyro_z1;

MFRC522 mfrc522(SS_PIN, RST_PIN);

int led_pin1=5;
int led_pin2=6;

Servo servo1;
Servo servo2;
Servo servo3;

int pos = 0;
int counter = 0;

boolean locked = true;

int buttonState = 0;  

void setup() {
  Serial.begin(9600); 
  
  Wire.begin();
  Wire.beginTransmission(MPU_ADDR); // Begins a transmission to the I2C slave (GY-521 board)
  Wire.write(0x6B); // PWR_MGMT_1 register
  Wire.write(0); // set to zero (wakes up the MPU-6050)
  gyro_x = Wire.read()<<8|Wire.read(); 
  gyro_y = Wire.read()<<8|Wire.read();  
  gyro_z = Wire.read()<<8|Wire.read(); 
  Wire.endTransmission(true);

  pinMode(buttonPin, INPUT);

  pinMode(red_light_pin, OUTPUT);
  pinMode(green_light_pin, OUTPUT);
  pinMode(blue_light_pin, OUTPUT);
  
//  digitalWrite(led_pin1,HIGH);
//  digitalWrite(led_pin2,LOW);
//  delay(2000);
//  digitalWrite(led_pin1,LOW);
//  digitalWrite(led_pin2,HIGH);
  RGB_color(255,0,0);

  servo1.attach(3);
  servo2.attach(2);
  servo3.attach(4);

  for (pos = 0; pos <= 170; pos += 1) { // goes from 0 degrees to 180 degrees
    servo2.write(pos); 
    delay(5);
  }
//  for (pos = 0; pos <= 200; pos += 1) { // goes from 0 degrees to 180 degrees
//    servo3.write(pos);   
//    delay(5);
//  }
//  for (pos = 200; pos >= 0; pos -= 1) { // goes from 0 degrees to 180 degrees
//    servo1.write(pos);  
//    delay(5);
//  }
 
  SPI.begin();      // Initiate  SPI bus
  mfrc522.PCD_Init(); 
  delay(2000);
}

void loop() {

//  while (bluetoothModule.available() > 0) {
//    input = bluetoothModule.read();
//  }
//  
//  while(input==0){
//    digitalWrite(led_pin,LOW);
//  }


  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x3B); // starting with register 0x3B (ACCEL_XOUT_H) [MPU-6000 and MPU-6050 Register Map and Descriptions Revision 4.2, p.40]
  Wire.endTransmission(false); // the parameter indicates that the Arduino will send a restart. As a result, the connection is kept active.
  Wire.requestFrom(MPU_ADDR, 12, true);

  gyro_x1 = Wire.read()<<8|Wire.read(); 
  gyro_y1 = Wire.read()<<8|Wire.read();  
  gyro_z1 = Wire.read()<<8|Wire.read();

  while ((abs(gyro_x1 - gyro_x) > 9000)||(abs(gyro_y1 - gyro_y) > 9000)||(abs(gyro_z1 - gyro_z) > 9000)) {
   Serial.println("Creeper Alert#");
   RGB_color(0,255,0);
   gyro_x = gyro_x1;
   gyro_y = gyro_y1;
   gyro_z = gyro_z1;
   delay(3000);
  }

  RGB_color(0,0,255);

  counter++;
   if ( ! mfrc522.PICC_IsNewCardPresent()) 
  {
    if (!locked && counter>30){
      for (pos = 0; pos <= 170; pos += 1) { // goes from 0 degrees to 180 degrees
        servo2.write(pos); 
        delay(5);
        locked = true;
      }
    }
    return;
  }
  // Select one of the cards
  if ( ! mfrc522.PICC_ReadCardSerial()) 
  {
    if (!locked && counter>30){
      for (pos = 0; pos <= 170; pos += 1) { // goes from 0 degrees to 180 degrees
        servo2.write(pos); 
        delay(5);
        locked = true; 
      }
    }
    return;
  }

  buttonState = digitalRead(buttonPin);
  
  String content= "";
  byte letter;
  for (byte i = 0; i < mfrc522.uid.size; i++) 
  {
     Serial.print(mfrc522.uid.uidByte[i] < 0x10 ? " 0" : " ");
     Serial.print(mfrc522.uid.uidByte[i], HEX);
     content.concat(String(mfrc522.uid.uidByte[i] < 0x10 ? " 0" : " "));
     content.concat(String(mfrc522.uid.uidByte[i], HEX));
  }
//  Serial.println();
  content.toUpperCase();
  while (content.substring(1) == "51 5F 27 1C" && locked==true) //change here the UID of the card/cards that you want to give access   //if
  {
    for (pos = 170; pos >= 0; pos -= 1) { // goes from 0 degrees to 180 degrees
      servo2.write(pos); 
      delay(5);
    }
    locked = false;
  }
  counter = 0;


  if (buttonState == HIGH) {
    // turn LED on:
    RGB_color(255,0,0);
  } else {
    // turn LED off:
    RGB_color(0,255,0);
  }
}

void RGB_color(int red_light_value, int green_light_value, int blue_light_value)
 {
  analogWrite(red_light_pin, red_light_value);
  analogWrite(green_light_pin, green_light_value);
  analogWrite(blue_light_pin, blue_light_value);
}
