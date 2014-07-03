package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StationListener extends Thread {

	protected final Set<Byte> usedSlotSet = Collections.synchronizedSet(new HashSet<Byte>());
	protected int timestampCount;
	protected long timestampAcc;
	protected static int TIMESTAMP_INDEX = 26;

	private MulticastSocket mcastSocket = null;
//	private DataSink dataSink = null;
	private InetAddress mcastAddress = null;
	private long frameStart;
	private byte lastSlot;
	private long receiveTimestamp;
	private boolean collision;
	private byte[] lastPacket = null;
	
	public StationListener(NetworkInterface netifc, InetAddress mcastAddress, int receivePort, DataSink dataSink) throws IOException {
//		this.dataSink = dataSink;
		this.mcastSocket = new MulticastSocket(receivePort);
		this.mcastAddress = mcastAddress;
		mcastSocket.setNetworkInterface(netifc);
		mcastSocket.joinGroup(mcastAddress);
	}
	
	private void process(byte[] data) {
		if (data != null) {
			String stationClass = (char) (data[0] & 0xFF) + "";
			ByteBuffer wrapped = ByteBuffer.wrap(data);
			long timestamp = wrapped.getLong(TIMESTAMP_INDEX);
			// <logging>
			// try {
			// String msg = new String(Arrays.copyOfRange(data, 1, 25),
			// "UTF-8");
			// dataSink.logInfoMassage("Received packet : stationClass: "
			// + stationClass + " / Data: " + msg + " / nextSlot: "
			// + slotNr + " / timestamp: " + timestamp);
			// } catch (UnsupportedEncodingException e) {
			// e.printStackTrace();
			// }
			// </logging>
			if (stationClass.equals("A")) {
				timestampAcc += timestamp - receiveTimestamp;
				timestampCount++;
			}
		}
	}
	
	private byte getSlot() {
		long time = Station.getTime();
		return (byte) (Math.floor((time - frameStart) / Station.SLOT_LENGTH) + 1);
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				byte[] data = new byte[Station.PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(data, data.length);
				mcastSocket.receive(packet);
				byte activeSlot = getSlot();
				collision = activeSlot == lastSlot;
				if (!collision){
						process(lastPacket);
				}
				receiveTimestamp = Station.getTime();
				lastPacket = data;
				byte slotNr = data[25];
				synchronized (usedSlotSet) {
					usedSlotSet.add(slotNr);
				  }
				lastSlot = activeSlot;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public long getNewOffset() {
//		if (timestampAcc > 0) {
		if (!collision){
			process(lastPacket);
		}
		long newTime = Math.round(timestampAcc / (double) timestampCount);
		return newTime;
//		}
//		else 
//			return Station.offset;
//		return (timestampCount > 0) ?time + Math.round(timestampAcc / (double) timestampCount) : time;
	}
	
	public void startFrame() {
		lastPacket = null;
		timestampCount = 1;
		timestampAcc = Station.offset;
		frameStart = Station.getTime();
		lastSlot = 0;
		usedSlotSet.clear();
	}

	public void close() {
		try {
			mcastSocket.leaveGroup(mcastAddress);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			mcastSocket.close();
		}
	}
}
