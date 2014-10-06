/********************************************************************* 
Project:You Watch My Stuff
Team Members: Randy, Amruta, Richard
Date: 11/21/2013
Definition: This program was created for YWMSy BLE device. The code
accomplishes certain tasks such as: 
  
  1) Detecting movement using an accelerometer
  2) Detecting sound using a mic
  3) Displaying an activated alarm by flashing a multi color LED
  4) and making a buzz sound when alarm has been activate

YWMSy BLE alarm has a BLE bluetooth device attached to communication 
between YWMSy BLE and iPhone/Android device. To handle the Bluetooth 
communication between BLE device and microcontroller the SoftwareSerial
Library was used.
Revision: 1
*********************************************************************
*********************************************************************
Copyright (C) 2013  
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free SoftwareFoundation, 
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
**********************************************************************/
#include "Arduino.h"
#include <SoftwareSerial.h>
#include <Tones.h>
//global costant used for Pin assignment 
const int RX_pin = 4;    // the pin Input for the Rx pin on bluetooth
const int TX_pin = 5;    // the pin output for the Tx pin on bluetooth
const int Red = 6;       // LED pin for red output
const int Green = 7;     // LED pin for green output
const int Blue = 8;      // LED pin for blue output
const int deviation = 40; // this is a +9 or -9 the values for accelerometer drift 
const int xpin = A0;     // x-axis input of the accelerometer
const int ypin = A1;     // y-axis input of the accelerometer
const int zpin = A2;     // z-axis input of the accelerometer
const int mic = A3;      // microphone input to microcontroller ATmega328P
const int Speaker_pin = 9;

// library used for bluetooth communication (bit banging)
SoftwareSerial Bluetooth(RX_pin,TX_pin);

// stuct for variables used on logic on communication
struct Alarm
{
int track;        // track variable keps state if blue or red ligh needs to flash
bool turnon;      // turn on lets logical enable keep track if the device is ON
bool exit_alarm;  // keeps the state if user has asked to exit from alarm state
bool enabled;
bool triggered;
char Phone_data;  // Variable for data received from phone
char data;        // Variable  to keep track of witch command send from phone
};

// struc for variables used with the accelerometer
struct accel
{
  int x_axis;    // keeps the x value for for reading a continous movement
  int y_axis;    // keeps the y value for for reading a continous movement
  int z_axis;    // keeps the z value for for reading a continous movement
  int init_x;    // This compensates for inital reading for accelerometer(know hardware issue) 
  int init_y;    // This compensates for inital reading for accelerometer(know hardware issue)
  int init_z;    // This compensates for inital reading for accelerometer(know hardware issue)
  int varUpperX; // keeps the x upper value to keep track of movement ex: if x_axis > varUpper_x + deviation then sound alarm 
  int varLowerX; // keeps the x lower value to keep track of movement ex: if x_axis < varLower_x - deviation then sound alarm
  int varUpperY; // keeps the y upper value to keep track of movement ex: if y_axis > varUpper_y + deviation then sound alarm
  int varLowerY; // keeps the y lower value to keep track of movement ex: if y_axis < varLower_y - deviation then sound alarm
  int varUpperZ; // keeps the z upper value to keep track of movement ex: if z_axis > varUpper_z + deviation then sound alarm
  int varLowerZ; // keeps the z lower value to keep track of movement ex: if z_axis < varLower_z - deviation then sound alarm
};

struct microphone
{
  int micVar;
  int micInit;
  int micUpper;
  int micLower;
};

Alarm alarm; // instance for the alarm struc
accel accel; // instance for the accelerometer struc
microphone micphn;
void setup()
{

    // seting up analog inputs for the accelerometer
  pinMode(xpin,INPUT);
  pinMode(ypin,INPUT);
  pinMode(zpin,INPUT);
  
  // set up of I/O ports for the bluetooth device 
  pinMode(TX_pin, OUTPUT);// setup Output TX pin
  pinMode(RX_pin, INPUT);
  // setting up the ouputs to light up Multi color LED
  pinMode(Red,OUTPUT);
  pinMode(Blue,OUTPUT);
  pinMode(Green,OUTPUT);
  
  //initialize the variables for logic on alarm 
  // this makes sure that Alarm starts at OFF state
  // and when alram sounds the Red LED will bright first
  alarm.track = 0;
  alarm.turnon = false;
  alarm.exit_alarm = false;
  
  // initializing the serial for Serial monitor for debugging 
  Serial.begin(9600);
  // initializing the bluetooth serial communication at 9600 Baud Rare
  Bluetooth.begin(19200);
  attachInterrupt(0,ISRON,CHANGE);
  //attachInterrupt(0,turn_off,FALLING);
}
int test = 1;
bool tester = true;
void loop()
{
  //Run();
  
}

void ISRON()
{
    Run();
}

void SensingSound()
{
  LED_off();
  LED_red();// turn on red LED
  micphn.micInit = analogRead(mic);
  micphn.micUpper = micphn.micInit;
  micphn.micLower = micphn.micInit;
  for(int i = 0; i < 20; ++i)// Sensing the Environment for 5 sec
 {
     micphn.micVar = analogRead(mic); 
     if(micphn.micVar > micphn.micUpper)
     {
       micphn.micUpper = micphn.micVar;
     }
     if(micphn.micVar < micphn.micLower)
     {
       micphn.micLower = micphn.micVar;
     }
  delay(250); // one second delay
 }
 LED_off();
}



// test the accelerometer 
void test_accel()
{
  int accelX = analogRead(xpin);
  int accelY = analogRead(ypin);
  int accelZ = analogRead(zpin);
  Serial.print("The X axis: ");
  Serial.print(accelX);
  Serial.print(" The Y axis: ");
  Serial.print(accelY);
  Serial.print(" The Z axis: ");
  Serial.print(accelZ);
  Serial.print('\n');
  delay(500);
}

void test_LEDs()
{
  LED_off();
  delay(1000);
  LED_red();
  delay(1000);
  LED_off();
  delay(1000);
  LED_green();
  delay(1000);
  LED_off();
  delay(1000);
  LED_blue();
  delay(1000);
}

void Run()
{
  turn_on();
  IntelRing();
  int dontbreakLoop = true;
  while(dontbreakLoop)
  {
// start by listeting for bluetooth device 
  Bluetooth.listen();
  // catch the responce if the device has been called to receive data
  bool listening = Bluetooth.isListening();
  // it its true then means that bluetooth is not transmitting data so ready to receive data
  if (listening == true)
  {
    // is bluetooth ready to receive data? if true that read in command send in via 
    // bluetooth in constant loop (polling) in function call loop
    if(Bluetooth.available()!= 0)
    {
      // since bluetooth is listening then read in any data to execute the correct command
      // if not specific data has been received that do nothing commands are based on char's
      // for example char a = turn on the device, b =  turn off device etc..
      read_char();
      switch (alarm.data)
      {        
        //case 'a'://Power on device (LED)
        //turn_on();
        //IntelRing();
        //break;
 
        case 'b'://Power off device(LED)
        turn_off();
        dontbreakLoop = false;
        tester = true;
        break;
        
        case 'c':
        // only execute when the alarm has been turned on otherwise ignore command
        if(alarm.turnon)
        {
          AlarmEnabled();
          // this function allows the accelerometer to read data for 5 seconds to decifer 
          // the environment that device is in. if it is a constant vibration then alarm 
          // will tolarate the vibration. this 5 sec read always happends when the alarm
          // has been enabled
          //Environment_Sensing();
          // loop that keeps device in the alarm Enabled Mode
          //LED_blue();
          //alarm.enabled = true;
          //do
          //{
            // turns on first loop and second time loop it turns off continues to do this
            // until the alarm has exit the alarm enabled mode or when the alarm has been 
            // triggered due to movement. i did this fucntion to use less power when
            // lighting up the LED since most of the time the alarm is in used is in the 
            // enabled mode.
            // monitor the x and y dirrection on accelerometer constatly to see if device has 
            // been moved
            //accel.x_axis = analogRead(xpin);
            //accel.y_axis = analogRead(ypin);
            //if the sensor has been moved execute and sound the alarm  
            //if(( accel.x_axis > (accel.varUpperX+deviation) ) || ( accel.y_axis > (accel.varUpperY+deviation)  )  || ( accel.x_axis < (accel.varLowerX-deviation) ) || ( accel.y_axis < (accel.varLowerY-deviation) ))
           // {
              // this is to send char f for android program to execute the alarm ringtone on phone
              //send_char();
            //  alarm.triggered = true;
              // continously sound the alarm until user disabled the alarm on phone app
           //   do
           //   {
                // sound the alarm and blink LED red and blue 
          //      sound_alarm();
          //      send_char();
                // read any command that phone might have send in to turn off alarm
          //      alarm.exit_alarm = check_exit(); 
          //    }while(alarm.exit_alarm != true);
          //    LED_off();
          //  }
          // read any command that phone might have send in to turn off alarm
         // alarm.exit_alarm = check_exit();
        //  }while(alarm.exit_alarm!= true);
        //  alarm.enabled = false;
        //  alarm.triggered = false;
        }
        break;
        
        case 'e':
        // automaticly sound alarm until user disables it via cell phone this is Panic/ Finder on phone
        do{
          sound_alarm();
          alarm.exit_alarm = check_exit();
        }while(alarm.exit_alarm != true);
        break;
        
        case 'g':
        turn_on();
        IntelRing();
        AlarmEnabled();
        break;
 
        default:
        break;
      }
    }
  }
  }
}

void AlarmEnabled()
{
// this function allows the accelerometer to read data for 5 seconds to decifer 
          // the environment that device is in. if it is a constant vibration then alarm 
          // will tolarate the vibration. this 5 sec read always happends when the alarm
          // has been enabled
          Environment_Sensing();
          // loop that keeps device in the alarm Enabled Mode
          //LED_blue();
          alarm.enabled = true;
          do
          {
            // turns on first loop and second time loop it turns off continues to do this
            // until the alarm has exit the alarm enabled mode or when the alarm has been 
            // triggered due to movement. i did this fucntion to use less power when
            // lighting up the LED since most of the time the alarm is in used is in the 
            // enabled mode.
            // monitor the x and y dirrection on accelerometer constatly to see if device has 
            // been moved
            accel.x_axis = analogRead(xpin);
            accel.y_axis = analogRead(ypin);
            //if the sensor has been moved execute and sound the alarm  
            if(( accel.x_axis > (accel.varUpperX+deviation) ) || ( accel.y_axis > (accel.varUpperY+deviation)  )  || ( accel.x_axis < (accel.varLowerX-deviation) ) || ( accel.y_axis < (accel.varLowerY-deviation) ))
            {
              // this is to send char f for android program to execute the alarm ringtone on phone
              //send_char();
              alarm.triggered = true;
              // continously sound the alarm until user disabled the alarm on phone app
              do
              {
                // sound the alarm and blink LED red and blue 
                sound_alarm();
                send_char();
                // read any command that phone might have send in to turn off alarm
                alarm.exit_alarm = check_exit(); 
              }while(alarm.exit_alarm != true);
              LED_off();
            }
          // read any command that phone might have send in to turn off alarm
          alarm.exit_alarm = check_exit();
          }while(alarm.exit_alarm!= true);
          alarm.enabled = false;
          alarm.triggered = false;
}
// turns off the LED by setting LOW to OUTPUT on LED
void LED_off()
{
  digitalWrite(Red,LOW);
  digitalWrite(Green,LOW);
  digitalWrite(Blue,LOW);
}

// Turns on the red color on LED
void LED_red()
{
  digitalWrite(Red,HIGH);
}
// Turns on the green color on LED
void LED_green()
{
  digitalWrite(Green,HIGH);
}

// Turns on the blue color on LED
void LED_blue()
{
  digitalWrite(Blue,HIGH);
}
// Turns on the speaker 
void beep()
{
  // turns on the speaker at frequency 2650 Hz in Pin 9 and duration is .300 sec
  // twice
  for (int thisNote = 0; thisNote < 1; thisNote++) 
  {
    tone(Speaker_pin,2000,100);
  }
}

// this blinks the blue LED half time and other half turns off
// this called on  the alarm enabled section to reduce the power
// used by the Blue LED
void blink_blue()
{
  if(alarm.track == 0)
  {
    LED_blue();
    alarm.track = 1;
  }
  if(alarm.track == 1)
  {
    LED_off();
    alarm.track = 0;
  } 
}

// this is the Alarm Blinking LED where half time is blinking blue 
// and the other half is blinking Red for a period of .6 sec
void LED_blink()
{
  if(alarm.track == 0)
  {
    LED_red();
    alarm.track = 1;
    delay(300);
    LED_off();
  }
  if(alarm.track == 1)
  {
    LED_blue();
    alarm.track = 0;
    delay(300);
    LED_off();
  } 
}
// turns off the LED and stops the tone from buzzer 
void turn_off()
{
  LED_off();
  noTone(Speaker_pin);
  alarm.turnon = false;
  alarm.enabled = true;
  alarm.triggered = true;
}

// blinks the LED red and blue and sounds alarm 
// when alarm has been triggered
void sound_alarm()
{
 LED_blink();
 beep();
}
// is called when the device has been turned on 
// it turns on the green LED and makes sure there 
// is no tone coming out of the buzzer
void turn_on()
{
  noTone(Speaker_pin);
  
  
    cli();          // disable global interrupts
    TCCR1A = 0;     // set entire TCCR1A register to 0
    TCCR1B = 0;     // same for TCCR1B
 
    // set compare match register to desired timer count:
    OCR1A = 15624;
    // turn on CTC mode:
    TCCR1B |= (1 << WGM12);
    // Set CS10 and CS12 bits for 1024 prescaler:
    TCCR1B |= (1 << CS10);
    TCCR1B |= (1 << CS12);
    // enable timer compare interrupt:
    TIMSK1 |= (1 << OCIE1A);
    // enable global interrupts:
    sei();

  alarm.turnon = true;
  alarm.enabled = false;
  alarm.triggered = false;
}

// this is the read coomand from bluetooth device 
// it reads anything that is send to the bluetooth 
// and keeps the char in a variable for future compare
// this is used to determine the logic of what command
// to exucute when send from the phone app
void read_char()
{
  alarm.Phone_data = Bluetooth.read();
  // compare if its char a
  if(alarm.Phone_data=='a')
  alarm.data = 'a'; // set the variable to be used to a
  // compare if its char a
  if(alarm.Phone_data=='b')
  alarm.data = 'b';// set the variable to be used to b
  // compare if its char a
  if(alarm.Phone_data=='c')
  alarm.data = 'c';// set the variable to be used to c
  // compare if its char a
  if(alarm.Phone_data=='d')
  alarm.data = 'd';// set the variable to be used to d
  // compare if its char a
  if(alarm.Phone_data=='e')
  alarm.data = 'e';// set the variable to be used to e
  if(alarm.Phone_data=='g')
  alarm.data = 'g';// set the variable to be used to e
}

// send data thru bluetooth. in this case it send the letter
// f to let the phone know that the alarm has been triggered 
// and to enable the ringtone on phone
void send_char()
{
    Bluetooth.write('ffffffff');// send the command to bluetooth
}

// read in command if user has called b or d char to exit enable mode = d or
// to turn off device = b. if exit enable mode then the alarm device will 
// automaticlly will go to turned on device.
bool check_exit()
{
  read_char();
  if(alarm.data == 'b')
  {
    LED_off();
    turn_off();
    return true;
  }
  if(alarm.data == 'd')
  {
    LED_off();
    turn_on();
    return true;
  }
  return false;
}
// this reads in and flushes and data from the accelerometer 
// thru debug it was found the the first couple of reads from 
// acceleromter where incorrect readings. did not find why just
// added this part to flush the incorrect readings
void read_init()
{
  accel.x_axis = analogRead(xpin);
  accel.y_axis = analogRead(ypin);
  accel.z_axis = analogRead(zpin);
  accel.varUpperX = accel.x_axis;
  accel.varLowerX = accel.varUpperX;
  accel.varUpperY = accel.y_axis;
  accel.varLowerY = accel.varUpperY;
  accel.varUpperZ = accel.z_axis;
  accel.varLowerZ = accel.varUpperZ;
}

// adjust the upper and lower levels to trigger alarm.
// every axis has a upper and lower axis so for example 
// if the alarm was being transported in car then the device
// will adjust itself to the vibrations fo the car upper and lower 
// readings this is called "Environment Sensing". to let user know
// the period when alarm is doinf this it turns on the red LED at beging 
// and turns off when it has completed with Sensing the Environment.
void Environment_Sensing()
{
  LED_red();// turn on red LED
  read_init();// flush the accelerometer
  for(int i = 0; i < 5; ++i)// Sensing the Environment for 5 sec
 {
   accel.x_axis = analogRead(xpin);
   accel.y_axis = analogRead(ypin);
   accel.z_axis = analogRead(ypin); 
   if(accel.x_axis > accel.varUpperX)
   {
     accel.varUpperX = accel.x_axis;
   }
   if(accel.x_axis < accel.varLowerX)
   {
     accel.varLowerX = accel.x_axis;
   }
   if(accel.y_axis > accel.varUpperY)
   {
     accel.varUpperY = accel.y_axis;
   }
   if(accel.y_axis < accel.varLowerY)
   {
     accel.varLowerY = accel.y_axis;
   }
   if(accel.z_axis > accel.varUpperZ)
   {
     accel.varUpperZ = accel.z_axis;
   }
   if(accel.z_axis < accel.varLowerZ)
   {
     accel.varLowerZ = accel.z_axis;
   }   
   delay(1000); // one second delay
   bool checker = check_exit();
   if(checker)
   break;
 }
 LED_off();
}


ISR(TIMER1_COMPA_vect)
{
  if(alarm.turnon & ~alarm.enabled & ~alarm.triggered){
    digitalWrite(Green, !digitalRead(Green));
  }
    if(alarm.enabled & ~alarm.triggered)
    digitalWrite(Blue, !digitalRead(Blue));
    if(alarm.triggered)
    {
      send_char();
      alarm.enabled = false;
    }
}

void IntelRing()
{
    tone(Speaker_pin,NOTE_DS4);
    delay(1000);
    noTone(Speaker_pin);
    delay(60);
    tone(Speaker_pin,NOTE_GS4);
    delay(200);
    noTone(Speaker_pin);
    delay(5);
    tone(Speaker_pin,NOTE_DS4);
    delay(200);
    noTone(Speaker_pin);
    delay(5);
    tone(Speaker_pin,NOTE_AS4);
    delay(400);
    noTone(Speaker_pin);
    delay(2000);
}
