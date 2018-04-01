/**
 * - Reads sensor data from ESP8266/MPU6050 via UDP
 * - Precomputes these sensor readings
 * - Forwards the values to the registered ValueSink
 * 
 * Author: Thomas Schuett, roboshock.de
 * 
 * License: Free to use in any way. No warrenty. Please leave 
 *          a note about the author name, thank you.
 */
package de.roboshock.javafx.gyropuppet;

import de.roboshock.device.DeviceReaderItf;
import de.roboshock.device.DeviceReader_ESP_UDP;

public class DeviceReaderThread extends Thread {

	private boolean debug = false;

	private static class ValueHolder {
		public double x;
		public double y;
		public double z;
	}

	final ValueHolder rotate = new ValueHolder();
	final ValueHolder gyroBias = new ValueHolder();
	ValueSinkDouble6 valueSink6;
	boolean terminationRequested = false;

	DeviceReaderItf deviceReader = new DeviceReader_ESP_UDP();
	double ax, ay, az = 0;
	double gx, gy, gz = 0;
	double gyroNoise = 200;

	@SuppressWarnings("serial")
	private static class MovementException extends Exception {
	}

	public DeviceReaderThread(ValueSinkDouble6 valueSink) {
		valueSink6 = valueSink;
	}

	public void run() {
		// deviceReader.openDevice("/dev/ttyUSB1", 38400);
		deviceReader.openPort("192.168.0.120", 1112);

		try {
			// initialize bias
			while (true) {
				try {
					initBias();
					finetuneBias();
					break;
				} catch (MovementException e) {
					System.out
							.println("Movement during sensor initialize phase ... retrying.");
					// o.k., try again
				}
			}
			System.out.println("Sensor initialized.");

			while (!terminationRequested) {

				nextReading();

				double scaleGyro = 9.0 / 7.0 / 1000d; // lead to degree 0 .. 90
														// .. 180
				double scaleAccel = 6.0 / 10000d; // --> acceleration values in
													// g (9.81)

				rotate.x = (gx - gyroBias.x) * scaleGyro;
				rotate.y = (gy - gyroBias.y) * scaleGyro;
				rotate.z = (gz - gyroBias.z) * scaleGyro;

				if (debug) {
					System.out
							.print(String
									.format("%-6d  %-6d  %-6d    gx(Bias): %-6d (%-6d)(%-6d)   gy(Bias): %-6d (%-6d)(%-6d)   gz(Bias): %-6d (%-6d)(%-6d) \n",
											(int) rotate.x, (int) rotate.y,
											(int) rotate.z, (int) gx,
											(int) gyroBias.x,
											(int) (gx - gyroBias.x), (int) gy,
											(int) gyroBias.y,
											(int) (gy - gyroBias.y), (int) gz,
											(int) gyroBias.z,
											(int) (gz - gyroBias.z)));
				}

				/**
				 * ax, ay, az: acceleration in m/s² (e.g. summarized 9.81) gx,
				 * gy, gz: Changes in degree (0 .. 45 .. 180) since last
				 * reading, relative to the sensor axes (actually orientation
				 * change speed in degrees per 20 ms, to be precise)
				 */
				if (valueSink6 != null) {
					valueSink6.setNewValues(ax * scaleAccel, ay * scaleAccel,
							az * scaleAccel, rotate.x, rotate.y, rotate.z);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		deviceReader.close();
		System.out.println("Reader thread ended.");
	}

	public void terminate() {
		terminationRequested = true;
	}

	// 0 +/- 500 is just noise, even +/- 3000 can be noise level
	// "real" rotation speed values may be +/- 30000
	private void initBias() {
		gyroBias.x = 0;
		gyroBias.y = 0;
		gyroBias.z = 0;
		int readings = 0;
		int trys = 0;
		while (readings < 10 && trys < 20) {
			nextReading();
			// System.out.println(readings+"  gx="+ gx + "   bias.x="+bias.x);
			trys++;
			if (gx == 0)
				continue;
			if (gy == 0)
				continue;
			if (gz == 0)
				continue;
			gyroBias.x += gx;
			gyroBias.y += gy;
			gyroBias.z += gz;
			readings++;
		}
		gyroBias.x /= readings;
		gyroBias.y /= readings;
		gyroBias.z /= readings;
		System.out.println("bias.x=" + gyroBias.x);
	}

	private void finetuneBias() throws MovementException {
		double sumx = 0;
		double sumy = 0;
		double sumz = 0;
		for (int i = 0; i < 100; i++) {
			nextReading();
			if (Math.abs(gx - gyroBias.x) > gyroNoise)
				throw new MovementException();
			if (Math.abs(gy - gyroBias.y) > gyroNoise)
				throw new MovementException();
			if (Math.abs(gz - gyroBias.z) > gyroNoise)
				throw new MovementException();
			sumx += gx;
			sumy += gy;
			sumz += gz;
		}
		gyroBias.x = sumx / 100.0;
		gyroBias.y = sumy / 100.0;
		gyroBias.z = sumz / 100.0;
	}

	private void nextReading() {
		String s = deviceReader.readLine_blocking();
		if (s.trim().length() == 0)
			return;
		String[] vals = s.split("[\\t ]+");
		if (vals.length != 6) {
			System.out.println("Expected 6 values: " + s);
			return;
		}
		// System.out.println(s);

		try {
			ax = Double.parseDouble(vals[0].substring(3));
			ay = Double.parseDouble(vals[1].substring(3));
			az = Double.parseDouble(vals[2].substring(3));
			gx = Double.parseDouble(vals[3].substring(3));
			gy = Double.parseDouble(vals[4].substring(3));
			gz = Double.parseDouble(vals[5].substring(3));
		} catch (NumberFormatException e) {
			System.out.println(s);
			return;
		}
	}

}
