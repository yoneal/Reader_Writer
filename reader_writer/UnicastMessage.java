/**
 * 
 */
package reader_writer;

import java.net.InetAddress;

import reader_writer.message.Message;

/**
 * @author neal
 *
 */
public class UnicastMessage 
{
	protected Message mMessage;
	protected InetAddress mDestAddr;
	protected int mDestPort;
	
	public Message getMessage()
	{
		return mMessage;
	}
	
	public InetAddress getDestAddr()
	{
		return mDestAddr;
	}
	
	public int getDestPort()
	{
		return mDestPort;
	}
	
	public void setMessage(Message m)
	{
		mMessage = m;
	}
	
	public void setDestAddr(InetAddress iad)
	{
		mDestAddr = iad;
	}
	
	public void setDestPort(int port)
	{
		mDestPort = port;
	}
}
