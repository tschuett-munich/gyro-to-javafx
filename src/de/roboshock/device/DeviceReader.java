package de.roboshock.device;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DeviceReader implements DeviceReaderItf {

	FileInputStream fis;
	
	public void openDevice(String deviceName, int baudrate) {
		try {
			exec("stty -F " + deviceName  + " " + baudrate);

			//String fullConfig9600 = "400:5:cbd:8a33:3:1c:7f:15:4:0:1:0:11:13:1a:0:12:f:17:16:0:0:0:0:0:0:0:0:0:0:0:0:0:0:0:0";
            //exec("stty -F " + portName  + " " + fullConfig9600);
			//exec("stty -F " + portName  + " " + baudrate + " -echo -icrnl");
			
            try { Thread.sleep(400); } catch (Exception ex) { ex.printStackTrace(); }
		
            System.out.println(".");
			fis = new FileInputStream(deviceName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private int exec (String bef) {
        try
        {
         Runtime rt = Runtime.getRuntime() ;
         Process p = rt.exec(bef) ;
         return p.waitFor();
        }
        catch(Exception ex){
                ex.printStackTrace();
                return -1;
        }
    }
	
	public int read() {
		while (true) {
			try {
				int x = fis.read();
				if (x < 0) {
					Thread.sleep(1);
				}
				else return x;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String readLine_blocking() {
		StringBuffer line = new StringBuffer();
		while (true) {
			int x = read();
			char c = (char) x;
			// c &= 0x7F;
			if (c == '\r') continue;
			if (c == '\n') return line.toString();
			else line.append(c);
		}
	}

	public void close() {
		try {
			fis.close();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

	//for testing
	public static void main(String[] args) {
		String portName = "/dev/ttyUSB1";
		DeviceReaderItf reader = new DeviceReader();
		reader.openDevice(portName, 38400);

		while (true) {
			String s = reader.readLine_blocking();
			System.out.println("XXX "+s);
		}
	}

	@Override
	public void openPort(String hostName, int port) {
		throw new RuntimeException("Not implemented - use openDevice instead");
	}

}
