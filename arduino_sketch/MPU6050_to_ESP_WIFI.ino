// ======================================================================
//
// This is the communication counterpart to 
//   Transport_UDP.java and DeviceReader_ESP_UDP.java
//
// Use ESP8266
//
// Have the MPU6050 connected to A4 (SDA), A5 (SCL)
// Use 3,3 V supply for the MPU6050
//
// Get lib from https://github.com/jrowberg/i2cdevlib/tree/master/Arduino/MPU6050
//
// You can attach a serial monitor to the USB with 115200 baud to see the WiFi connect info.
//
// Author: Thomas Schuett, thomas-at-roboshock.de
// 
// Licence: Free to use in any way. No warrenty. Please leave 
//          a note about the author name, thank you.
//

// ======================================================================

char hello[] = "MPU6050_to_ESP_WIFI, Arduino 1.6.8 (ts)";

unsigned long lastTime = 0;

// ======= MPU-6050 ============
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include "I2Cdev.h"
#include "MPU6050.h"
#include "Wire.h"

boolean verbose = true;

char appendTmp[200];

int blink_speed = 20;
int blink_count = 0;
int led = 13;

// I2C pins:
// Uno, Ethernet   A4 (SDA), A5 (SCL)
// Mega2560        20 (SDA), 21 (SCL)
// ESP            GPIO4 D2 (SDA),  GPIO5 D1 (SCL)

MPU6050 accelgyro;
int16_t ax, ay, az;
int16_t gx, gy, gz;
boolean mpu6050connected = false;

// ============================================================================

// WLAN
#define wifi_ssid "XXXXXXXXXXXX"
#define wifi_password "XXXXXXXXXXX"

// UDP
WiFiUDP Udp;
int portSelf = 1112;
IPAddress remoteIP;
long remotePort = 0;
boolean connected = false;

// For TCP version: send data must be buffered, to make sure it will all be sent in one packet
// For UDP version: everything for one datagram must be collected anyway
char outbuf[512];
int outbufLen = 512;
int outbufFill = 0;

// ring buffer for receive data
char inbuf[256];
int inbufLen = 256;
int inbufNextReadPos = 0;
int inbufNextWritePos = 0;


// we want to be able to peek the next char from the input
char peekedEthernetChar;
boolean peekFilled = false;


// ============================================================================

void setup()
{
  delay(2000);

  outbuf[0] = 0;
  outbufFill = 0;
  inbuf[0] = 0;

  // ====== UARTs =============
  Serial.begin(115200);
  //Serial1.begin(115200);
  //Serial.swap();
  //Serial.set_tx(2);
  Serial.println(hello);


  // ======= WLAN and UDP  ==============
  setup_wifi();
  Udp.begin(portSelf);

  // ============ pins ======================

  pinMode(led, OUTPUT);

  setup_mpu();
  
}

void setup_mpu() {
  // ======= MPU-6050 ============
  // ESP:
  // GPIO4 D2 SDA
  // GPIO5 D1 SCL
  Wire.begin(4, 5); // sda, scl
  accelgyro.initialize();
  if (accelgyro.testConnection()) {
     mpu6050connected = true;
  }
}

void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(wifi_ssid);

  Serial.println();
  //Serial.println("Debug output is on Serial1, not Serial.");

  WiFi.begin(wifi_ssid, wifi_password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());
  Serial.println();
  Serial.print("Waiting for UDP instruction packets on port ");
  Serial.println(portSelf);
}


void udp_write() {
      Udp.beginPacket(remoteIP, remotePort);
      Udp.write(outbuf, outbufFill);
      Udp.endPacket();
      outbufFill = 0;
      outbuf[0] = 0;
}

void loop()
{
  if (connected) {

    // print welcome msg
    int h1 = accelgyro.getRate();
    int h2 = accelgyro.getDLPFMode();
    
    char floatStr[10];
    dtostrf(h1,1,0,floatStr); send("Sample rate: "); send (floatStr);
    dtostrf(h2,1,0,floatStr); send("    DLPF mode: "); send (floatStr);
    send("\n");
    udp_write();

    accelgyro.setRate(1);
    accelgyro.setDLPFMode(3);
    accelgyro.setFullScaleGyroRange(3);

    h1 = accelgyro.getRate();
    h2 = accelgyro.getDLPFMode();
    dtostrf(h1,1,0,floatStr); send("Sample rate: "); send (floatStr);
    dtostrf(h2,1,0,floatStr); send("    DLPF mode: "); send (floatStr);
    send("\n");
    udp_write();

     while (connected) { 
       
        outbuf[0] = 0;
        outbufFill = 0;

        while (millis() < lastTime + 20) {
          yield();
        }
        lastTime = millis();

     
        append6050data();  
        //blink();

        while (get_one_instruction_from_ethernet()) {
        }

        if (outbufFill > 0) {
          //appendToOutbuf("EoP.\n");
          udp_write();
        }

        yield();
        
    } // while connected

  }
  else {  // if not connected
     // look for UDP instruction packets, maybe a connect instruction
     while (get_one_instruction_from_ethernet()) {
      delay(1); // does a yield() internaly
      if (connected) {
        break;
      }
     }
  }
  
  delay(20);

} // end of loop


void append6050data() {
  if (! mpu6050connected) {
    send("I:MPU-6050 NOT connected.\n");
  }
  else {
    accelgyro.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);

    // sometimes my MPU6050 forgets its settings (power supply problem?)
    char floatStr[10];
    int retry = 0;
    while (accelgyro.getFullScaleGyroRange() != 3) {
      accelgyro.setFullScaleGyroRange(3);
      accelgyro.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
      retry++;
      send("XXXX XXXX XXXX XXXX gyro scale range resetted ");
      dtostrf(retry,1,0,floatStr);
      send(floatStr);
      send("\n");
      if (retry == 3) {
        gx = 0; gy = 0; gz = 0; 
        break;
      }
    }

    dtostrf(ax,1,0,floatStr);            if (verbose) send("ax="); send(floatStr);
    dtostrf(ay,1,0,floatStr); send(" "); if (verbose) send("ay="); send(floatStr);
    dtostrf(az,1,0,floatStr); send(" "); if (verbose) send("az="); send(floatStr);
    dtostrf(gx,1,0,floatStr); send(" "); if (verbose) send("gx="); send(floatStr);
    dtostrf(gy,1,0,floatStr); send(" "); if (verbose) send("gy="); send(floatStr); 
    dtostrf(gz,1,0,floatStr); send(" "); if (verbose) send("gz="); send(floatStr); 
    send("\n");

    // sometimes my MPU6050 forgets its settings (power supply problem?)
    if (gx == 0 && gy == 0 && gz == 0 && ax == 0 && ay == 0 && az == 0) {
      send("XXXX XXXX XXXX XXX gyro sleep resetted.\n");
      //accelgyro.initialize();
      accelgyro.setSleepEnabled(false);
    }

  }
}
 
void send(String in) {
  in.toCharArray(appendTmp, in.length() + 1); //Copy the string (+1 for the terminating null char)
  send_wifi(appendTmp);
}

void send_uart(char in[]) {
  for (int pos = 0; pos < 200; pos++) {
    if (in[pos] == '\0') break;
    Serial.write(in[pos]);
  }
}

void send_wifi(char in[]) {
   appendToOutbuf(in);
}

void blink() {
  blink_count++;
  if (blink_count == blink_speed) {
    blink_count = 0;
    digitalWrite(led, ! digitalRead(led));
  }  
}

void appendToOutbuf(char in[]) {
  int pos2;
  // kopieren
  for (pos2 = 0; pos2 < 200; pos2++) {
    outbuf[outbufFill] = in[pos2];
    if (in[pos2] == '\0') break;
    outbufFill++;
    if (outbufFill == outbufLen) {
      outbuf[outbufFill] = '\0';
      break;
    }
  }
  
  // or: sprintf(outbuf+strlen(outbuf),"### Interrupt timer underrun ###\n");
  //     sprintf(outbuf+strlen(outbuf),"I:Cmds_sent=%d  ", instructions_sent);
}

// ====================================================================

// returns, if there may be further commands to be executed now
boolean get_one_instruction_from_ethernet() {  

    if (! isMsgAvailable()) { return false; }
        
    char serPort;      
    char c = client_peek();

    if (c == 'A') { // read arduino analog in line
       client_read();  // consume this char
       client_consumeToLineEnd();
       sprintf(outbuf+strlen(outbuf),"\n");
       return true;
    }

    else   if (c == 'V') { // verbosity 0=more silent   1=verbose
       client_read();  // consume this char
       int onOff = client_readInteger();
       client_consumeToLineEnd();
       return true;
    }

    else if ((c == 'c') && !connected) {  // connect me at port xxxx (e.g. "c1234")
         remoteIP = Udp.remoteIP();

         remotePort = atol(inbuf+1);
         Serial.print("Connected to ");
         Serial.print(remoteIP);
         Serial.print(", port ");
         Serial.println(remotePort);
         connected = true;
         client_consumeToLineEnd();
         delay(1);
         return true;
    }

    else if (c == 'x') {  // cut connection
         connected = false;
         client_consumeToLineEnd();
         Serial.println("UDP closed ");
         delay(1);
         return true;
    }

    else {
       // ignore everything till next \n
       while (c != '\n') {
          c = client_read();
       }
       return false;
    }
}

int client_readInteger() {
   client_consumeBlanks();
   char intStr[10];
   int pos = 0;
   intStr[pos] = 0;
   while (true) {
      char c = client_peek();
      if ( c >= 0x30 && c <= 0x39) {
        intStr[pos] = c;
        intStr[pos+1] = 0;
        pos++;
        client_read();  // consume it
        if (pos >= 8) break;
      }
      else {
        break;
      }
   }
   return atoi(intStr);
}

void client_consumeBlanks() {
   while (true) {
      char c = client_peek();
      if ( c == ' ') {
        client_read();  // consume it
      }
      else {
        break;
      }
   }
}

void client_consumeToLineEnd() {
   char  c = client_read();
   // ignore everything till next \n
   while (c != '\n') {
     c = client_read();
   }
}

// ==========================================================

uint8_t localBuf[256];

void fillReadBuffer() {
      int packetSize = Udp.parsePacket();
      if (! packetSize) {
        return;
      }

      // read the packet into inbuf
      // (with UDP, inbuf will always be filled up to line boundaries,
      // so if fillReadBuffer() is called, we can be sure that there is nothing left
      // in inbuf, so we can just use it from pos 0 (instead of using it as a ring buffer)
      int len = Udp.read(inbuf, 255);
      inbuf[len] = 0; // string terminating zero
      inbufNextReadPos = 0;
      inbufNextWritePos = len+1;
}


boolean isMsgAvailable_in_inbuf() {
  int nextReadPos = inbufNextReadPos;
  while (nextReadPos != inbufNextWritePos) {
    if (inbuf[nextReadPos] == '\n') return true;
    if (inbuf[nextReadPos] == 0) return false;
    nextReadPos++;
  }
  return false;
}

boolean isMsgAvailable() {
  if (isMsgAvailable_in_inbuf()) {
    return true;
  }
  fillReadBuffer();
  return isMsgAvailable_in_inbuf();
}

char client_read() {
  if (inbufNextReadPos == inbufNextWritePos) return '\n'; // quick n dirty
    // (should never happen, as it will only be called after a isMsgAvailable() returned true)
  char ret = inbuf[inbufNextReadPos];
  inbufNextReadPos++;
  if (inbufNextReadPos == inbufLen) inbufNextReadPos = 0;
  return ret;
}

char client_peek() {
  if (inbufNextReadPos == inbufNextWritePos) return '\n'; // quick n dirty 
    // (should never happen, as it will only be called after a isMsgAvailable() returned true)
  return inbuf[inbufNextReadPos];
}

