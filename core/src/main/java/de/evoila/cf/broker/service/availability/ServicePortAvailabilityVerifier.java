/**
 * 
 */
package de.evoila.cf.broker.service.availability;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.evoila.cf.broker.exception.PlatformException;

/**
 * @author Johannes Hiemer.
 *
 */
public class ServicePortAvailabilityVerifier {

	private static final int SOCKET_TIMEOUT = 10000;

	private static final int INITIAL_TIMEOUT = 60 * 1000;
	
	//@Value("${backend.connection.timeouts}")
	private static final int connectionTimeouts = 18;

	private static Logger log = LoggerFactory.getLogger(ServicePortAvailabilityVerifier.class);

	public static void initialSleep() {
		try {
			Thread.sleep(INITIAL_TIMEOUT);
		} catch (InterruptedException e1) {
			log.info("Initial timeout was interrupted.", e1);
		}
	}

	public static boolean execute(String ip, int port) {
		boolean available = false;

		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(ip, port), SOCKET_TIMEOUT);

			if (socket.isConnected())
				available = true;
		} catch (Exception e) {
			log.info("Service port could not be reached", e);
		} finally {
			if (socket != null && socket.isConnected()) {
				try {
					socket.close();
				} catch (IOException e) {
					log.info("Could not close port", e);
				}
			}
		}
		return available;
	}
	
	public static boolean verifyServiceAvailability(String ip, int port) throws PlatformException {
		boolean available = false;

		ServicePortAvailabilityVerifier.initialSleep();
		for (int i = 0; i < connectionTimeouts; i++) {
			available = ServicePortAvailabilityVerifier.execute(ip, port);
			
			log.info("Service Port availability: " + available);

			if (available) {
				break;
			}
		}
		return available;
	}

}
