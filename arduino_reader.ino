double salinity, pressure;
//salinity output = 13, input = A5
//pressure output = 5V, input = A0

void setup(){
  Serial.begin(9600);
  pinMode(13, OUTPUT);
}//end setup

void loop(){
  digitalWrite(13, HIGH);//toggle salinity probe on
  delay(100);//wait for value to settle
  salinity = analogRead(A5);//read salinity probe
  digitalWrite(13, LOW);//toggle salinity probe off
  Serial.println(salinity);//send salinity probe reading to computer
  delay(400)
  pressure = analogRead(A0);//read pressure sensor
  pressure += 10000;//pressure values are identified in java by being over 10,000
  Serial.println(pressure);//send pressure sensor reading to computer
  delay(500);
}//end loop

