package de.roboshock.device;

public interface DeviceReaderItf {

	void openDevice(String deviceName, int baudrate);

	void openPort(String hostName, int port);

	String readLine_blocking();

	void close();

}