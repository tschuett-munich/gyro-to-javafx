/**
 * This is the communication counterpart to the 
 *   arduino sketch MPU6050_to_ESP_WIFI
 * 
 * Author: Thomas Schuett, roboshock.de
 * 
 * License: Free to use in any way. No warrenty. Please leave 
 *          a note about the author name, thank you.
 */
package de.roboshock.device;

public class DeviceReader_ESP_UDP implements DeviceReaderItf {

	private Transport_UDP transport;

	@Override
	public void openPort(String hostName, int port) {
    	transport = new Transport_UDP(hostName, port);

    	try {
    		transport.closeSocket(); // always close first
    		transport.openSocket();
    		System.out.println("Socket (re-) connected.");
    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
	}

	@Override
	public String readLine_blocking() {   // blocking
		
		try {
			// transport.isLineAvailable(): 
			//    If there is no line in the line buffer, then it tries to 
			//    get the next UDP packet, if available within a short timeout.
			while ( ! transport.isLineAvailable()) {
				Thread.sleep(1);
			}
			String line = transport.readLineFromBuffer_nonBlocking();
			return line;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String readLine_timeout_1ms() {
		
		try {
			if (transport.isLineAvailable()) { // if no line in buffer, then 
				                               // try to get one UDP packet into buffer, 
				                               // if available within a short timeout.
				String line = transport.readLineFromBuffer_nonBlocking();
				return line;
			}
			else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void close() {
		transport.writeLine("x");
	}

	@Override
	public void openDevice(String deviceName, int baudrate) {
		throw new RuntimeException("Not implemented - use openPort instead");
	}

}
