package main;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class DataSink {
	private static boolean LOGGING = false;
	private final static Logger logger = Logger.getLogger(DataSink.class.getName());
	private FileHandler fh = null;
	private long id;

	public DataSink(long id) {
		this.id = id;
	}

	public synchronized void logInfoMassage(String msg) {
		if (LOGGING) {
			try {
				this.fh = new FileHandler("DataSink" + id + ".log", true);
			} catch (SecurityException | IOException e) {
				e.printStackTrace();
			}
			Logger l = Logger.getLogger("");
			fh.setFormatter(new SimpleFormatter());
			l.addHandler(fh);
			l.setLevel(Level.CONFIG);

			logger.log(Level.INFO, msg + "\n");
			fh.close();
		}
	}
}
