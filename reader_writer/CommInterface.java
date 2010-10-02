/**
 * 
 */
package reader_writer;

import reader_writer.message.*;

/**
 * @author neal
 *
 */
public interface CommInterface 
{

	public int sendMessage(Message message);
	
	public Message receiveMessage();
}
