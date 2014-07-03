package main;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Station {
	protected static final int PACKET_SIZE = 34;
	protected static final byte SLOTS_PER_FRAME = 25;
	protected static final long FRAME_LENGTH = 1000;
	protected static final long SLOT_LENGTH = FRAME_LENGTH / (long) SLOTS_PER_FRAME;
	protected static final long SLOT_OFFSET = SLOT_LENGTH / 2;

	private DataSink dataSink;
	private StationListener listener = null;
	private StationSender sender = null;
	public static long offset;

	public Station(NetworkInterface netifc, InetAddress mcastAddress, int port, byte stationClass, long UTCoffset) throws IOException {
		this.dataSink = new DataSink(UTCoffset);
		this.listener = new StationListener(netifc, mcastAddress, port, dataSink);
		this.sender = new StationSender(netifc, mcastAddress, port, dataSink, stationClass, 1);
		offset = UTCoffset;
	}
	
	private byte getNextSlot() {
		Set<Byte> slotSet = new HashSet<Byte>();
		for (byte i = 1; i < SLOTS_PER_FRAME+1; i++) {
			slotSet.add(i);
		}
		synchronized (listener.usedSlotSet) {
		      Iterator<Byte> i = listener.usedSlotSet.iterator(); // Must be in the synchronized block
		      while (i.hasNext())
		  		slotSet.remove(i.next());
		  }
		int nextSlot = 1 + (int) (Math.random() * ((slotSet.size()  - 1) + 1));
		if (slotSet.toArray().length > 0)
			return (byte) slotSet.toArray()[nextSlot-1];
		else
			return 0;
	}
	
	protected static long getTime() {
		return System.currentTimeMillis() + offset;
	}
	
	public void runStation() throws IOException, InterruptedException {
		listener.start();
		listener.startFrame();
		Thread.sleep(FRAME_LENGTH - (getTime() % FRAME_LENGTH));
		listener.startFrame();
		Thread.sleep(FRAME_LENGTH);
		byte slot = getNextSlot();
		while(true) {
			long timeTillEndOfFrame = FRAME_LENGTH - (System.currentTimeMillis() % FRAME_LENGTH);
			if (timeTillEndOfFrame > 0) {
				Thread.sleep(timeTillEndOfFrame);
			}
			offset += listener.getNewOffset();

			listener.startFrame();
//			dataSink.logInfoMassage("New Frame at: " + offset);
			long sendTime = SLOT_LENGTH * slot - SLOT_OFFSET;
//			Thread.sleep(sendTime);
//			offset += listener.getNewOffset();
//			slot = getNextSlot();
//			sender.sendPacket(slot);
//			if (sendTime > offset) {
				Thread.sleep(Math.max((sendTime + offset) % FRAME_LENGTH, 0));
				slot = getNextSlot();
				if (slot > 0)
//					offset += listener.getNewOffset();
					sender.sendPacket(slot);
//				}
			
		}
	}
	
	public void close() {
		listener.close();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		String interfaceName = args[0];
		NetworkInterface netifc = NetworkInterface.getByName(interfaceName);
		InetAddress mcastAddress = null;
		try {
			mcastAddress = InetAddress.getByName(args[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int receivePort = Integer.parseInt(args[2]);
		byte stationClass = args[3].getBytes()[0];
		long UTCoffset =  args.length == 5 ? Long.parseLong(args[4]) : 0;
		
//		System.out.println("Network interface: " + interfaceName);
//		System.out.println("Mutlicast address: " + mcastAddress.toString());
//		System.out.println("Receive port: " + receivePort);
//		System.out.println("Station class: " + Byte.toString(stationClass));
//		Enumeration<NetworkInterface> ifcList = NetworkInterface.getNetworkInterfaces();
//		for (NetworkInterface ifc:Collections.list(ifcList)) {
//			System.out.println(ifc.toString());
//			for (InetAddress adr:Collections.list(ifc.getInetAddresses()))
//				System.out.println(adr.toString());
//			System.out.println("+++++++++++++++++++++++++++");
//		}
		
		DataSourceReader datasourceReader = new DataSourceReader();
		datasourceReader.start();
		final Station station = new Station(netifc, mcastAddress, receivePort, stationClass, UTCoffset);
		station.runStation();
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		        station.close();
		    }
		});
	}
}
