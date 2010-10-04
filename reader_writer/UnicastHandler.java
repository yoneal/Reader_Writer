/**
 * 
 */
package reader_writer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * @author Nerisse
 *
 */
public class UnicastHandler 
{

	
	/**
	 * Check if the specified port is available, shamelessly ripped from the Apache Mina project
	 *
	 * @param port the port to check for availability
	 * @return true if port is available and false if port is not available
	 */
	public static boolean available(int port) {
	    if (port < miMinPortNumber || port > miMaxPortNumber) {
	        throw new IllegalArgumentException("Invalid port number range: " + port);
	    }

	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
	}
	
}
