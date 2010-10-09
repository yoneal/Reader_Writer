/**
 * 
 */
package reader_writer;

import reader_writer.message.*;
import java.math.BigInteger;

/**
 * @author neal
 *
 */
public interface CommInterface 
{
	/**
	 * Send a message with the following sequence number
	 * @param message
	 * @param msg_seq
	 * @return
	 */
	public int sendMessage(Message message);
	/**
	 * Receive a multicast message
	 * @return
	 */
	public Message receiveMessage();
	/**
	 * Blocking call that waits for the interface to be ready
	 */
	public void connect();
}
