package main;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class StationSender {

	private InetAddress mcAddress = null;
	private int sendPort;
	private DataSink dataSink = null;
	private byte stationClass;
	private MulticastSocket mcsocket = null;
	
	public StationSender(NetworkInterface netifc, InetAddress mcastAddress, int port, DataSink dataSink, byte stationClass, int ttl) throws IOException {
		this.mcAddress = mcastAddress;
		this.sendPort = port;
		this.dataSink = dataSink;
		this.stationClass = stationClass;
		this.mcsocket = new MulticastSocket();
		this.mcsocket.setNetworkInterface(netifc);
		this.mcsocket.setTimeToLive(ttl);
	}

	private DatagramPacket createPacket(byte slot) {
		ByteBuffer packetBuffer = ByteBuffer.allocate(Station.PACKET_SIZE);
		byte[] data = DataSourceReader.getData();
		long timestamp = Station.getTime();
		packetBuffer.put(stationClass);
		packetBuffer.put(data);
		packetBuffer.put(slot);
		packetBuffer.putLong(timestamp);
		// <logging>
		try {
			String stationStr = (char) (stationClass & 0xFF) + "";
			String msgStr = new String(Arrays.copyOfRange(data, 0, DataSourceReader.DATA_LENGTH), "UTF-8");
			dataSink.logInfoMassage("Send packet: stationClass: " + stationStr + " / Data: " + msgStr + 
					" / nextSlot: " + slot + " / timestamp: " + timestamp);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		// </logging>
		DatagramPacket packet = new DatagramPacket(packetBuffer.array(), Station.PACKET_SIZE, mcAddress, sendPort);
		return packet;
	}

	public void sendPacket(byte slot) {
		try {
			DatagramPacket packet = createPacket(slot);
			mcsocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
