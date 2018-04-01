package de.roboshock.device;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * The UDP version to communicate with the robot
 */
public class Transport_UDP {
	private String hostName;
	private int port;
	private int portSelf = 2134;

	private StringBuffer socketbuf = new StringBuffer();

	private static DatagramSocket serverSocket;
	private boolean connected = false;

	public Transport_UDP(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;

		// disconnect on exit, so the ESP will stop looping
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				closeSocket();
			}
		});
	}

	public String getCommPortName() {
		return hostName + ":" + port;
	}

	public void write(String cmdString) {
		writeToSocket(cmdString);
	}

	public void writeLine(String cmdString) {
		writeToSocket(cmdString + "\n");
	}

	private void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * If the robot values are sent in a verbose form (e.g. " P12=314") or not
	 * (e.g. " 314")
	 */
	public void setVerbosity(boolean on) throws Exception {
		if (on)
			writeLine("V1");
		else
			writeLine("V0");
	}

	public void openSocket() {
		try {
			serverSocket = new DatagramSocket(portSelf);
			serverSocket.setSoTimeout(1);
			writeLine("c" + portSelf); // connect to ESP
			connected = true;
		} catch (Throwable e) {
			System.out.println("Can not open receiving UDP port (localhost:"
					+ portSelf + "): " + e.getMessage());
			System.out.println("Exiting now.");
			System.exit(-1);
		}
	}

	public void closeSocket() {
		try {
			// Always send close, to be sure. Do not check boolean "connected"
			System.out.println("Closing Socket ...");
			writeLine("x");
			connected = false;
			sleep(400);
		} catch (Exception e) {
			e.printStackTrace();
		}
		sleep(100);
	}

	public boolean isConnected() {
		return connected;
	}

	private void writeToSocket(String sendChars) {
		try {
			byte[] buffer = sendChars.getBytes();
			InetAddress address = InetAddress.getByName(hostName);
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
					address, port);
			DatagramSocket datagramSocket = new DatagramSocket();
			datagramSocket.send(packet);
			datagramSocket.close();

		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	public boolean isLineAvailable() throws IOException, Exception {
		if (isLineInBuffer()) {
			return true;
		}
		fillBufferFromSocket_timeout_1ms(); // takes time (timeout !) in the UDP
											// version
		return isLineInBuffer();
	}

	public boolean isLineInBuffer() throws IOException, Exception {
		return socketbuf.indexOf("\n") > -1;
	}

	public String readLineFromBuffer_nonBlocking() throws IOException,
			Exception {
		int idx = socketbuf.indexOf("\n");
		if (idx < 0)
			return "";
		if (idx == 0) {
			socketbuf.replace(0, 1, ""); // delete the \n
			return "";
		}

		String ret = socketbuf.substring(0, idx); // without \n
		socketbuf.replace(0, idx + 1, ""); // delete including the \n

		return ret;
	}

	/**
	 * returns one line WITHOUT any \n or \r
	 */
	private void fillBufferFromSocket_timeout_1ms() throws IOException,
			Exception {
		// socket timeout must be low (1 ms), otherwise there will
		// always be something more to receive !!!
		serverSocket.setSoTimeout(1);
		try {
			byte[] receiveData = new byte[512];
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			serverSocket.receive(receivePacket);
			convertToString(receivePacket.getData(), receivePacket.getLength());
		} catch (SocketTimeoutException e) {
			// do nothing
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void convertToString(byte[] receiveData, int len)
			throws IOException {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < len; i++) {
			byte l = receiveData[i];
			if ((char) l == '\r') {
				continue; // ignore
			}
			buffer.append((char) l);
		}
		;
		socketbuf.append(buffer);
	}

	public void dismissAllIncomingPackets() throws Exception {
		fillBufferFromSocket_timeout_1ms();
		try {
			while (socketbuf.length() > 0) {
				socketbuf.setLength(0);
				fillBufferFromSocket_timeout_1ms();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
