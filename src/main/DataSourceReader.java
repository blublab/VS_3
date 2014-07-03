package main;

import java.io.IOException;

public class DataSourceReader extends Thread {
	public static final int DATA_LENGTH = 24;
	private static volatile byte[]  data = new byte[DATA_LENGTH];
	
	public static byte[] getData() {
		return data;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				System.in.read(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
